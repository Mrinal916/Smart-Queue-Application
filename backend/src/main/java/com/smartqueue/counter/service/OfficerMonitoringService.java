package com.smartqueue.counter.service;

import com.smartqueue.common.exception.BusinessConflictException;
import com.smartqueue.common.exception.ResourceNotFoundException;
import com.smartqueue.counter.dto.OfficerCounterOptionResponse;
import com.smartqueue.counter.dto.OfficerDashboardResponse;
import com.smartqueue.counter.dto.QueueSummaryResponse;
import com.smartqueue.counter.entity.Counter;
import com.smartqueue.counter.repository.CounterServiceAssignmentRepository;
import com.smartqueue.counter.repository.OfficerCounterAssignmentRepository;
import com.smartqueue.token.dto.TokenResponse;
import com.smartqueue.token.entity.Token;
import com.smartqueue.token.enums.TokenStatus;
import com.smartqueue.token.repository.TokenRepository;
import com.smartqueue.user.enums.RoleName;
import com.smartqueue.user.repository.UserAccountRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficerMonitoringService {
  private final CounterService counters;
  private final OfficerCounterAssignmentRepository assignments;
  private final CounterServiceAssignmentRepository serviceAssignments;
  private final UserAccountRepository users;
  private final TokenRepository tokens;

  public OfficerMonitoringService(
      CounterService counters,
      OfficerCounterAssignmentRepository assignments,
      CounterServiceAssignmentRepository serviceAssignments,
      UserAccountRepository users,
      TokenRepository tokens) {
    this.counters = counters;
    this.assignments = assignments;
    this.serviceAssignments = serviceAssignments;
    this.users = users;
    this.tokens = tokens;
  }

  @Transactional(readOnly = true)
  public QueueSummaryResponse summary(UUID officerId, UUID counterId, UUID serviceId) {
    authorize(officerId, counterId);
    Counter counter = counters.get(counterId);
    List<TokenResponse> waiting =
        tokens
            .findAllByServicePublicIdAndQueueDateAndStatusOrderByAgePriorityDescTokenNumberAsc(
                serviceId, LocalDate.now(), TokenStatus.WAITING)
            .stream()
            .map(this::map)
            .toList();
    TokenResponse current =
        tokens
            .findByCounterPublicIdAndStatus(counterId, TokenStatus.CALLED)
            .map(this::map)
            .orElse(null);
    return new QueueSummaryResponse(counter.getPublicId(), current, waiting.size(), waiting);
  }

  @Transactional(readOnly = true)
  public OfficerDashboardResponse dashboard(
      UUID officerId,
      UUID counterId,
      UUID serviceId,
      LocalDate queueDate,
      Set<TokenStatus> statuses,
      Boolean arrived) {
    authorize(officerId, counterId);
    Counter counter = counters.get(counterId);
    var assignment =
        serviceAssignments.findAllByCounterPublicIdAndActiveTrue(counterId).stream()
            .filter(a -> a.getService().getPublicId().equals(serviceId))
            .findFirst()
            .orElseThrow(
                () -> new BusinessConflictException("This service is not assigned to the counter"));
    List<Token> all =
        tokens.findAllByServicePublicIdAndQueueDateOrderByAgePriorityDescTokenNumberAsc(
            serviceId, queueDate);
    TokenResponse current =
        all.stream()
            .filter(
                t ->
                    t.getStatus() == TokenStatus.CALLED
                        && t.getCounter() != null
                        && t.getCounter().getPublicId().equals(counterId))
            .findFirst()
            .map(this::map)
            .orElse(null);
    List<TokenResponse> filtered =
        all.stream()
            .filter(t -> statuses == null || statuses.isEmpty() || statuses.contains(t.getStatus()))
            .filter(t -> arrived == null || t.hasAppeared() == arrived)
            .map(this::map)
            .toList();
    long completed = all.stream().filter(t -> t.getStatus() == TokenStatus.COMPLETED).count();
    long cancelled = all.stream().filter(t -> t.getStatus() == TokenStatus.CANCELLED).count();
    long appeared = all.stream().filter(Token::hasAppeared).count();
    long waiting =
        all.stream()
            .filter(
                t -> t.getStatus() == TokenStatus.WAITING || t.getStatus() == TokenStatus.SKIPPED)
            .count();
    int averageWait =
        waiting == 0
            ? 0
            : Math.toIntExact(
                Math.round(
                    ((waiting - 1) * counterServiceAverageMinutes(counterId, serviceId)) / 2.0));
    return new OfficerDashboardResponse(
        counterId,
        counter.getCode(),
        counter.getOffice().getName(),
        assignment.getService().getName(),
        counter.getStatus().name(),
        queueDate,
        current,
        filtered,
        Math.toIntExact(waiting),
        completed,
        cancelled,
        appeared,
        averageWait);
  }

  private int counterServiceAverageMinutes(UUID counterId, UUID serviceId) {
    return serviceAssignments.findAllByCounterPublicIdAndActiveTrue(counterId).stream()
        .filter(a -> a.getService().getPublicId().equals(serviceId))
        .findFirst()
        .orElseThrow()
        .getService()
        .getAverageServiceMinutes();
  }

  @Transactional(readOnly = true)
  public Counter authorizeCounter(UUID officerId, UUID counterId) {
    authorize(officerId, counterId);
    return counters.get(counterId);
  }

  @Transactional(readOnly = true)
  public List<OfficerCounterOptionResponse> assignedCounters(UUID officerId) {
    return assignments.findAllByOfficerPublicIdAndReleasedAtIsNull(officerId).stream()
        .map(
            assignment -> {
              Counter counter = assignment.getCounter();
              var services =
                  serviceAssignments
                      .findAllByCounterPublicIdAndActiveTrue(counter.getPublicId())
                      .stream()
                      .map(
                          service ->
                              new OfficerCounterOptionResponse.AssignedService(
                                  service.getService().getPublicId(),
                                  service.getService().getName()))
                      .toList();
              return new OfficerCounterOptionResponse(
                  counter.getPublicId(),
                  counter.getCode(),
                  counter.getOffice().getName(),
                  counter.getOffice().getCategory(),
                  services);
            })
        .toList();
  }

  private void authorize(UUID officerId, UUID counterId) {
    var user =
        users
            .findByPublicId(officerId)
            .orElseThrow(() -> new ResourceNotFoundException("Officer", officerId));
    if (user.getRole().getName() != RoleName.OFFICER
        || !assignments.existsByOfficerPublicIdAndCounterPublicIdAndReleasedAtIsNull(
            officerId, counterId))
      throw new BusinessConflictException("Officer is not assigned to this counter");
  }

  private TokenResponse map(Token t) {
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
        t.getCounter() == null ? null : t.getCounter().getPublicId(),
        t.getCounter() == null ? null : t.getCounter().getCode());
  }
}
