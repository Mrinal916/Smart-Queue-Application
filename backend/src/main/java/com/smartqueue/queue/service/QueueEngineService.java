package com.smartqueue.queue.service;

import com.smartqueue.auth.service.JwtService;
import com.smartqueue.common.exception.BusinessConflictException;
import com.smartqueue.common.exception.ResourceNotFoundException;
import com.smartqueue.counter.entity.Counter;
import com.smartqueue.counter.repository.CounterServiceAssignmentRepository;
import com.smartqueue.counter.repository.OfficerCounterAssignmentRepository;
import com.smartqueue.counter.service.CounterService;
import com.smartqueue.notification.dto.TokenNotificationPayload;
import com.smartqueue.notification.service.NotificationClient;
import com.smartqueue.queue.config.RedisQueueProperties;
import com.smartqueue.queue.entity.QueueHistory;
import com.smartqueue.queue.exception.QueueOperationException;
import com.smartqueue.queue.repository.QueueHistoryRepository;
import com.smartqueue.servicecatalog.entity.QueueService;
import com.smartqueue.servicecatalog.repository.QueueServiceRepository;
import com.smartqueue.token.dto.LiveQueueStatusResponse;
import com.smartqueue.token.dto.QrCodeResponse;
import com.smartqueue.token.dto.TokenPageResponse;
import com.smartqueue.token.dto.TokenResponse;
import com.smartqueue.token.dto.WaitTimeResponse;
import com.smartqueue.token.entity.Token;
import com.smartqueue.token.enums.TokenStatus;
import com.smartqueue.token.repository.TokenRepository;
import com.smartqueue.token.service.TokenStateMachine;
import com.smartqueue.user.entity.UserAccount;
import com.smartqueue.user.enums.RoleName;
import com.smartqueue.user.repository.UserAccountRepository;
import com.smartqueue.websocket.event.QueueDomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueueEngineService {
  private static final Logger log = LoggerFactory.getLogger(QueueEngineService.class);
  private final TokenRepository tokens;
  private final QueueHistoryRepository history;
  private final QueueServiceRepository services;
  private final UserAccountRepository users;
  private final CounterService counters;
  private final OfficerCounterAssignmentRepository officerAssignments;
  private final CounterServiceAssignmentRepository serviceAssignments;
  private final StringRedisTemplate redis;
  private final RedisQueueProperties properties;
  private final TokenStateMachine stateMachine;
  private final JwtService jwtService;
  private final ApplicationEventPublisher eventPublisher;
  private final NotificationClient notificationClient;
  private final TokenTimeoutScheduler timeoutScheduler;

  public QueueEngineService(
      TokenRepository tokens,
      QueueHistoryRepository history,
      QueueServiceRepository services,
      UserAccountRepository users,
      CounterService counters,
      OfficerCounterAssignmentRepository officerAssignments,
      CounterServiceAssignmentRepository serviceAssignments,
      StringRedisTemplate redis,
      RedisQueueProperties properties,
      TokenStateMachine stateMachine,
      JwtService jwtService,
      ApplicationEventPublisher eventPublisher,
      NotificationClient notificationClient,
      TokenTimeoutScheduler timeoutScheduler) {
    this.tokens = tokens;
    this.history = history;
    this.services = services;
    this.users = users;
    this.counters = counters;
    this.officerAssignments = officerAssignments;
    this.serviceAssignments = serviceAssignments;
    this.redis = redis;
    this.properties = properties;
    this.stateMachine = stateMachine;
    this.jwtService = jwtService;
    this.eventPublisher = eventPublisher;
    this.notificationClient = notificationClient;
    this.timeoutScheduler = timeoutScheduler;
  }

  @Transactional
  public TokenResponse book(
      UUID citizenId,
      String visitorName,
      String visitorPhone,
      Integer visitorAge,
      String visitorGender,
      UUID serviceId,
      LocalDate appointmentDate,
      LocalTime appointmentTime,
      String bookingKey) {
    UserAccount citizen = user(citizenId);
    QueueService service =
        services
            .findLockedByPublicId(serviceId)
            .orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));
    Token existing = tokens.findByCitizenIdAndBookingKey(citizen.getId(), bookingKey).orElse(null);
    if (existing != null) return map(existing);
    if (!service.getDepartment().isActive() || !service.getDepartment().getOffice().isActive())
      throw new BusinessConflictException("Office or department is inactive");
    LocalDate today = LocalDate.now();
    if (appointmentDate.isBefore(today))
      throw new BusinessConflictException("Appointments cannot be booked for a past date");
    if (appointmentDate.isAfter(today.plusDays(30)))
      throw new BusinessConflictException("Bookings can be made up to 30 days in advance");
    if (!service.getOpenDays().contains(appointmentDate.getDayOfWeek()))
      throw new BusinessConflictException(
          "This service is closed on "
              + appointmentDate.getDayOfWeek().name().toLowerCase().replace('_', ' '));
    if (!validSlot(service, appointmentTime))
      throw new BusinessConflictException("Choose one of the available service time slots");
    if (appointmentDate.equals(today) && !appointmentTime.isAfter(LocalTime.now()))
      throw new BusinessConflictException("Choose a future time slot for today's appointment");
    if (tokens.existsByServiceIdAndQueueDateAndAppointmentTimeAndStatusIn(
        service.getId(),
        appointmentDate,
        appointmentTime,
        EnumSet.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.SKIPPED)))
      throw new BusinessConflictException(
          "This time slot has already been booked for the selected service");
    if (tokens.existsByCitizenIdAndOfficeIdAndQueueDateAndStatusIn(
        citizen.getId(),
        service.getDepartment().getOffice().getId(),
        appointmentDate,
        EnumSet.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.SKIPPED)))
      throw new BusinessConflictException(
          "You already have an active booking for this location on this date");
    long count = tokens.countByServiceIdAndQueueDate(service.getId(), appointmentDate);
    if (count >= service.getDailyCapacity())
      throw new BusinessConflictException("Service daily capacity has been reached");
    Token token =
        tokens.save(
            new Token(
                citizen,
                service.getDepartment().getOffice(),
                service,
                0,
                bookingKey,
                appointmentDate,
                appointmentTime,
                visitorName.trim(),
                visitorPhone.trim(),
                visitorAge,
                visitorGender));
    token.assignSerialNumber(token.getId());
    history.save(new QueueHistory(token, null, TokenStatus.WAITING, citizen, null));
    add(token);
    timeoutScheduler.scheduleCancellation(token);
    eventPublisher.publishEvent(
        new QueueDomainEvent(
            "TOKEN_BOOKED",
            token.getOffice().getPublicId(),
            token.getService().getPublicId(),
            token.getPublicId(),
            citizen.getPublicId(),
            TokenStatus.WAITING.name(),
            Instant.now()));

    // Dispatch booking confirmation email
    try {
      notificationClient.sendConfirmationEmail(
          TokenNotificationPayload.confirmation(
              citizen.getEmail(),
              visitorName.trim(),
              String.valueOf(token.getTokenNumber()),
              service.getName(),
              service.getDepartment().getOffice().getName(),
              service.getDepartment().getName(),
              appointmentDate.toString(),
              appointmentTime != null ? appointmentTime.toString() : ""));
    } catch (Exception ex) {
      log.error("Failed to send booking confirmation email: {}", ex.getMessage());
    }

    return map(token);
  }

  @Transactional(readOnly = true)
  public List<LocalTime> availableSlots(UUID serviceId, LocalDate appointmentDate) {
    QueueService service =
        services
            .findByPublicIdAndActiveTrue(serviceId)
            .orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));
    if (appointmentDate.isBefore(LocalDate.now())
        || appointmentDate.isAfter(LocalDate.now().plusDays(30)))
      throw new BusinessConflictException("Bookings can be made up to 30 days in advance");
    if (!service.getOpenDays().contains(appointmentDate.getDayOfWeek())) return List.of();
    Set<LocalTime> booked =
        tokens
            .findAllByServiceIdAndQueueDateAndStatusIn(
                service.getId(),
                appointmentDate,
                EnumSet.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.SKIPPED))
            .stream()
            .map(Token::getAppointmentTime)
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());
    List<LocalTime> slots = new java.util.ArrayList<>();
    for (LocalTime slot = service.getStartTime();
        slot.isBefore(service.getEndTime());
        slot = slot.plusMinutes(service.getAverageServiceMinutes())) {
      if (validSlot(service, slot)
          && !booked.contains(slot)
          && (!appointmentDate.equals(LocalDate.now()) || slot.isAfter(LocalTime.now())))
        slots.add(slot);
    }
    return slots;
  }

  @Transactional(readOnly = true)
  public TokenResponse checkIn(UUID citizenId, UUID tokenId) {
    Token t = token(tokenId);
    if (!t.getCitizen().getPublicId().equals(citizenId))
      throw new BusinessConflictException("Citizen cannot access another citizen's token");
    return map(t);
  }

  @Transactional
  public TokenResponse markArrived(UUID officerId, UUID counterId, UUID tokenId) {
    Counter c = authorizedCounter(officerId, counterId);
    Token t = token(tokenId);
    validateCounterForToken(c, t);
    if (t.getStatus() != TokenStatus.WAITING && t.getStatus() != TokenStatus.SKIPPED)
      throw new BusinessConflictException(
          "Only an active waiting appointment can be marked as arrived");
    t.markAppeared();
    timeoutScheduler.cancelCancellation(tokenId);
    eventPublisher.publishEvent(
        new QueueDomainEvent(
            "TOKEN_ARRIVED",
            t.getOffice().getPublicId(),
            t.getService().getPublicId(),
            t.getPublicId(),
            t.getCitizen().getPublicId(),
            t.getStatus().name(),
            Instant.now()));
    return map(t);
  }

  @Transactional(readOnly = true)
  public TokenResponse activeToken(UUID citizenId) {
    UserAccount citizen = user(citizenId);
    Token token =
        tokens
            .findTopByCitizenIdAndStatusInOrderByCreatedAtDesc(
                citizen.getId(),
                EnumSet.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.SKIPPED))
            .orElseThrow(() -> new ResourceNotFoundException("Active token", citizenId));
    return map(token);
  }

  @Transactional(readOnly = true)
  public TokenPageResponse history(UUID citizenId, int page, int size) {
    UserAccount citizen = user(citizenId);
    var result =
        tokens.findAllByCitizenIdOrderByCreatedAtDesc(citizen.getId(), PageRequest.of(page, size));
    return new TokenPageResponse(
        result.getContent().stream().map(this::map).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Transactional(readOnly = true)
  public LiveQueueStatusResponse liveStatus(UUID serviceId) {
    TokenResponse current =
        tokens
            .findTopByServicePublicIdAndQueueDateAndStatusOrderByTokenNumber(
                serviceId, LocalDate.now(), TokenStatus.CALLED)
            .map(this::map)
            .orElse(null);
    long waiting =
        tokens.countByServicePublicIdAndQueueDateAndStatus(
            serviceId, LocalDate.now(), TokenStatus.WAITING);
    return new LiveQueueStatusResponse(serviceId, current, Math.toIntExact(waiting));
  }

  @Transactional
  public TokenResponse next(UUID officerId, UUID counterId, UUID serviceId) {
    Counter c = authorizedCounter(officerId, counterId);
    if (!serviceAssignments.existsByCounterPublicIdAndServicePublicIdAndActiveTrue(
        counterId, serviceId))
      throw new BusinessConflictException("Counter is not assigned to this service");
    TypedTuple<String> tuple =
        redis.opsForZSet().popMin(properties.keyPrefix() + ":" + serviceId + ":" + LocalDate.now());
    if (tuple == null) throw new QueueOperationException("Queue is empty");
    Token t = token(UUID.fromString(tuple.getValue()));
    if (t.getStatus() != TokenStatus.WAITING && t.getStatus() != TokenStatus.SKIPPED)
      throw new QueueOperationException("Token is no longer available to call");
    timeoutScheduler.cancelCancellation(t.getPublicId());
    transition(t, TokenStatus.CALLED, c, user(officerId));
    timeoutScheduler.scheduleCompletion(t);
    return map(t);
  }

  @Transactional
  public TokenResponse skip(UUID officerId, UUID counterId, UUID tokenId) {
    Counter c = authorizedCounter(officerId, counterId);
    Token t = token(tokenId);
    validateCounterForToken(c, t);
    timeoutScheduler.cancelCompletion(tokenId);
    transition(t, TokenStatus.SKIPPED, c, user(officerId));
    add(t);
    return map(t);
  }

  @Transactional
  public TokenResponse recall(UUID officerId, UUID counterId, UUID tokenId) {
    Counter c = authorizedCounter(officerId, counterId);
    Token t = token(tokenId);
    validateCounterForToken(c, t);
    timeoutScheduler.cancelCancellation(tokenId);
    transition(t, TokenStatus.CALLED, c, user(officerId));
    remove(t);
    timeoutScheduler.scheduleCompletion(t);
    return map(t);
  }

  @Transactional
  public TokenResponse complete(UUID officerId, UUID counterId, UUID tokenId) {
    Counter c = authorizedCounter(officerId, counterId);
    Token t = token(tokenId);
    validateCounterForToken(c, t);
    transition(t, TokenStatus.COMPLETED, c, user(officerId));
    remove(t);
    return map(t);
  }

  @Transactional
  public TokenResponse noShow(UUID officerId, UUID counterId, UUID tokenId) {
    Counter c = authorizedCounter(officerId, counterId);
    Token t = token(tokenId);
    validateCounterForToken(c, t);
    transition(t, TokenStatus.NO_SHOW, c, user(officerId));
    remove(t);
    return map(t);
  }

  @Transactional
  public TokenResponse cancel(UUID citizenId, UUID tokenId) {
    Token t = token(tokenId);
    if (!t.getCitizen().getPublicId().equals(citizenId))
      throw new BusinessConflictException("Citizen cannot access another citizen's token");
    if (t.getStatus() != TokenStatus.WAITING)
      throw new BusinessConflictException(
          "Only a waiting token can be cancelled. Please contact the service desk if you have"
              + " already been called.");
    transition(t, TokenStatus.CANCELLED, null, t.getCitizen());
    remove(t);
    return map(t);
  }

  @Transactional
  public void cancelIfGracePeriodElapsed(UUID tokenId) {
    Token token = token(tokenId);
    if (token.getStatus() != TokenStatus.WAITING && token.getStatus() != TokenStatus.SKIPPED)
      return;
    int interval = token.getService().getAverageServiceMinutes();
    if (interval < 10 || token.hasAppeared() || token.getAppointmentTime() == null) return;
    long graceMinutes = (long) Math.ceil(interval / 2.0);
    Instant deadline =
        LocalDateTime.of(token.getQueueDate(), token.getAppointmentTime())
            .plusMinutes(graceMinutes)
            .atZone(ZoneId.systemDefault())
            .toInstant();
    if (!Instant.now().isBefore(deadline)) {
      transition(token, TokenStatus.CANCELLED, null, null);
      remove(token);
    }
  }

  @Transactional
  public void completeIfServiceTimeElapsed(UUID tokenId) {
    Token token = token(tokenId);
    if (token.getStatus() != TokenStatus.CALLED || token.getCheckedInAt() == null) return;
    Instant deadline =
        token.getCheckedInAt().plusSeconds(token.getService().getAverageServiceMinutes() * 60L);
    if (!Instant.now().isBefore(deadline)) {
      transition(token, TokenStatus.COMPLETED, token.getCounter(), null);
      remove(token);
    }
  }

  @Transactional(readOnly = true)
  public WaitTimeResponse waitTime(UUID citizenId, UUID tokenId) {
    Token t = token(tokenId);
    if (!t.getCitizen().getPublicId().equals(citizenId))
      throw new BusinessConflictException("Citizen cannot access another citizen's token");
    var allActive =
        tokens.findAllByServiceIdAndQueueDateAndStatusInOrderByAgePriorityDescTokenNumberAsc(
            t.getService().getId(),
            t.getQueueDate(),
            EnumSet.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.SKIPPED));
    var relevantQueue =
        t.getQueueDate().equals(LocalDate.now())
            ? allActive.stream()
                .filter(
                    item ->
                        item.getAppointmentTime() == null
                            || !item.getAppointmentTime().isBefore(LocalTime.now()))
                .toList()
            : allActive;
    int ahead =
        java.util.stream.IntStream.range(0, relevantQueue.size())
            .filter(index -> relevantQueue.get(index).getPublicId().equals(t.getPublicId()))
            .findFirst()
            .orElse(0);
    long queueMinutes = (long) ahead * t.getService().getAverageServiceMinutes();
    long untilAppointment =
        t.getAppointmentTime() == null
            ? 0
            : Math.max(
                0,
                java.time.Duration.between(
                        java.time.LocalDateTime.now(),
                        java.time.LocalDateTime.of(t.getQueueDate(), t.getAppointmentTime()))
                    .toMinutes());
    return new WaitTimeResponse(
        t.getPublicId(), ahead, Math.toIntExact(Math.max(queueMinutes, untilAppointment)));
  }

  @Transactional(readOnly = true)
  public QrCodeResponse qrCode(UUID citizenId, UUID tokenId) {
    Token t = token(tokenId);
    if (!t.getCitizen().getPublicId().equals(citizenId) || t.getStatus() != TokenStatus.WAITING)
      throw new BusinessConflictException("QR is available only for an active waiting token");
    return new QrCodeResponse(
        tokenId, jwtService.createQrCheckInToken(tokenId), jwtService.qrExpiresAt());
  }

  @Transactional(readOnly = true)
  public TokenResponse validateQrCheckIn(UUID citizenId, String payload) {
    Token t = token(jwtService.extractQrCheckInTokenId(payload));
    if (!t.getCitizen().getPublicId().equals(citizenId) || t.getStatus() != TokenStatus.WAITING)
      throw new BusinessConflictException("QR token is not valid for check-in");
    return map(t);
  }

  private void transition(Token t, TokenStatus next, Counter c, UserAccount actor) {
    TokenStatus prior = t.getStatus();
    stateMachine.validate(prior, next);
    t.transition(next, c);
    if (next == TokenStatus.COMPLETED
        || next == TokenStatus.NO_SHOW
        || next == TokenStatus.CANCELLED) timeoutScheduler.cancelAll(t.getPublicId());
    history.save(new QueueHistory(t, prior, next, actor, c));
    eventPublisher.publishEvent(
        new QueueDomainEvent(
            "TOKEN_" + next,
            t.getOffice().getPublicId(),
            t.getService().getPublicId(),
            t.getPublicId(),
            t.getCitizen().getPublicId(),
            next.name(),
            java.time.Instant.now()));
    log.info(
        "Token {} transitioned from {} to {} by {}",
        t.getPublicId(),
        prior,
        next,
        actor == null ? "automatic timeout" : actor.getPublicId());

    // Dispatch token status update email (CALLED, SERVED, CANCELLED, SKIPPED)
    try {
      String counterName = (c != null) ? (c.getOffice().getName() + " — " + c.getCode()) : null;
      String recipientName =
          t.getVisitorName() != null && !t.getVisitorName().isBlank()
              ? t.getVisitorName()
              : t.getCitizen().getEmail().split("@")[0];
      notificationClient.sendStatusUpdateEmail(
          TokenNotificationPayload.statusUpdate(
              t.getCitizen().getEmail(),
              recipientName,
              String.valueOf(t.getTokenNumber()),
              t.getService().getName(),
              next.name(),
              counterName,
              "Token #" + t.getTokenNumber() + " is now " + next.name()));
    } catch (Exception ex) {
      log.error("Failed to send token status update email: {}", ex.getMessage());
    }
  }

  private Counter authorizedCounter(UUID officerId, UUID counterId) {
    UserAccount u = user(officerId);
    if (u.getRole().getName() != RoleName.OFFICER
        || !officerAssignments.existsByOfficerPublicIdAndCounterPublicIdAndReleasedAtIsNull(
            officerId, counterId))
      throw new BusinessConflictException(
          "Only the officer assigned to this counter can operate tokens");
    Counter c = counters.get(counterId);
    if (!c.isActive() || c.getStatus() != com.smartqueue.counter.enums.CounterStatus.OPEN)
      throw new BusinessConflictException("Counter is closed");
    return c;
  }

  private void validateCounterForToken(Counter counter, Token token) {
    if (!counter.getOffice().getId().equals(token.getOffice().getId())
        || !serviceAssignments.existsByCounterPublicIdAndServicePublicIdAndActiveTrue(
            counter.getPublicId(), token.getService().getPublicId()))
      throw new BusinessConflictException("Counter is not assigned to this token's service");
    if (token.getStatus() == TokenStatus.CALLED
        && (token.getCounter() == null || !token.getCounter().getId().equals(counter.getId())))
      throw new BusinessConflictException("Token is being served at a different counter");
  }

  private UserAccount user(UUID id) {
    return users.findByPublicId(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
  }

  private Token token(UUID id) {
    return tokens.findByPublicId(id).orElseThrow(() -> new ResourceNotFoundException("Token", id));
  }

  private boolean validSlot(QueueService s, LocalTime time) {
    if (time.isBefore(s.getStartTime()) || !time.isBefore(s.getEndTime()) || duringBreak(s, time))
      return false;
    long minutes = java.time.Duration.between(s.getStartTime(), time).toMinutes();
    return minutes % s.getAverageServiceMinutes() == 0;
  }

  private boolean duringBreak(QueueService s, LocalTime time) {
    return s.getBreakStartTime() != null
        && !time.isBefore(s.getBreakStartTime())
        && time.isBefore(s.getBreakEndTime());
  }

  private String key(Token t) {
    return properties.keyPrefix() + ":" + t.getService().getPublicId() + ":" + t.getQueueDate();
  }

  private double queueScore(Token t) {
    return t.hasAgePriority() ? t.getTokenNumber() - 1_000_000d : t.getTokenNumber();
  }

  private void add(Token t) {
    String key = key(t);
    redis.opsForZSet().add(key, t.getPublicId().toString(), queueScore(t));
    redis.expire(key, properties.ttl());
  }

  private void remove(Token t) {
    redis.opsForZSet().remove(key(t), t.getPublicId().toString());
  }

  private TokenResponse map(Token t) {
    Counter displayCounter = t.getCounter();
    if (displayCounter == null
        && t.getAppointmentTime() != null
        && !LocalDateTime.of(t.getQueueDate(), t.getAppointmentTime())
            .isAfter(LocalDateTime.now().plusHours(1)))
      displayCounter =
          serviceAssignments
              .findFirstByServicePublicIdAndActiveTrueOrderByCounterCodeAsc(
                  t.getService().getPublicId())
              .map(com.smartqueue.counter.entity.CounterServiceAssignment::getCounter)
              .orElse(null);
    return new TokenResponse(
        t.getPublicId(),
        t.getTokenNumber(),
        t.getQueueDate(),
        t.getAppointmentTime(),
        t.getStatus(),
        t.getService().getPublicId(),
        t.getOffice().getPublicId(),
        t.getOffice().getName(),
        t.getOffice().getAddress(),
        t.getService().getDepartment().getName(),
        t.getService().getName(),
        t.getVisitorName(),
        t.getVisitorPhone(),
        t.getVisitorAge(),
        t.getVisitorGender(),
        t.hasAgePriority(),
        t.hasAppeared(),
        t.getAppearedAt(),
        displayCounter == null ? null : displayCounter.getPublicId(),
        displayCounter == null ? null : displayCounter.getCode());
  }
}
