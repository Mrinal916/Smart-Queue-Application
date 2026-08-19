package com.smartqueue.counter.service;

import com.smartqueue.common.exception.*;
import com.smartqueue.counter.dto.*;
import com.smartqueue.counter.entity.*;
import com.smartqueue.counter.enums.CounterStatus;
import com.smartqueue.counter.repository.*;
import com.smartqueue.office.service.OfficeService;
import com.smartqueue.servicecatalog.service.QueueServiceManagementService;
import com.smartqueue.user.entity.UserAccount;
import com.smartqueue.user.enums.RoleName;
import com.smartqueue.user.repository.UserAccountRepository;
import com.smartqueue.websocket.event.CounterDomainEvent;
import java.util.*;
import org.slf4j.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CounterService {
  private final CounterRepository counters;
  private final OfficerCounterAssignmentRepository officerAssignments;
  private final CounterServiceAssignmentRepository serviceAssignments;
  private final OfficeService offices;
  private final QueueServiceManagementService services;
  private final UserAccountRepository users;
  private final ApplicationEventPublisher eventPublisher;
  private final CounterStatusHistoryRepository statusHistory;
  private static final Logger log = LoggerFactory.getLogger(CounterService.class);

  public CounterService(
      CounterRepository counters,
      OfficerCounterAssignmentRepository officerAssignments,
      CounterServiceAssignmentRepository serviceAssignments,
      OfficeService offices,
      QueueServiceManagementService services,
      UserAccountRepository users,
      ApplicationEventPublisher eventPublisher,
      CounterStatusHistoryRepository statusHistory) {
    this.counters = counters;
    this.officerAssignments = officerAssignments;
    this.serviceAssignments = serviceAssignments;
    this.offices = offices;
    this.services = services;
    this.users = users;
    this.eventPublisher = eventPublisher;
    this.statusHistory = statusHistory;
  }

  @Transactional
  public CounterResponse create(CounterRequest r) {
    var office = offices.get(r.officeId());
    String code = normalizeCode(r.code());
    if (counters.existsByOfficeIdAndCodeIgnoreCase(office.getId(), code))
      throw new BusinessConflictException(
          "A counter with code '" + code + "' already exists in this office");
    return map(counters.save(new Counter(office, code)));
  }

  @Transactional
  public CounterResponse update(UUID id, CounterRequest r) {
    Counter c = get(id);
    var office = offices.get(r.officeId());
    String code = normalizeCode(r.code());
    if (counters.existsByOfficeIdAndCodeIgnoreCaseAndPublicIdNot(office.getId(), code, id))
      throw new BusinessConflictException(
          "A counter with code '" + code + "' already exists in this office");
    c.update(office, code, true);
    return map(c);
  }

  @Transactional
  public void delete(UUID id) {
    Counter c = get(id);
    if (c.getStatus() == CounterStatus.OPEN)
      throw new BusinessConflictException("Close the counter before deactivating it");
    officerAssignments
        .findByCounterPublicIdAndReleasedAtIsNull(id)
        .ifPresent(OfficerCounterAssignment::release);
    serviceAssignments
        .findAllByCounterPublicIdAndActiveTrue(id)
        .forEach(CounterServiceAssignment::release);
    c.deactivate();
  }

  @Transactional
  public CounterResponse open(UUID id) {
    Counter c = get(id);
    c.setStatus(CounterStatus.OPEN);
    statusHistory.save(new CounterStatusHistory(c, CounterStatus.OPEN));
    eventPublisher.publishEvent(
        new CounterDomainEvent(
            c.getOffice().getPublicId(),
            c.getPublicId(),
            CounterStatus.OPEN.name(),
            java.time.Instant.now()));
    log.info("Counter opened: {}", id);
    return map(c);
  }

  @Transactional
  public CounterResponse close(UUID id) {
    Counter c = get(id);
    c.setStatus(CounterStatus.CLOSED);
    statusHistory.save(new CounterStatusHistory(c, CounterStatus.CLOSED));
    eventPublisher.publishEvent(
        new CounterDomainEvent(
            c.getOffice().getPublicId(),
            c.getPublicId(),
            CounterStatus.CLOSED.name(),
            java.time.Instant.now()));
    log.info("Counter closed: {}", id);
    return map(c);
  }

  @Transactional(readOnly = true)
  public CounterResponse getResponse(UUID id) {
    return map(get(id));
  }

  @Transactional(readOnly = true)
  public List<CounterResponse> list(UUID officeId) {
    offices.get(officeId);
    return counters.findAllByOfficePublicIdAndActiveTrueOrderByCode(officeId).stream()
        .map(this::map)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CounterManagementResponse> managementList(UUID officeId) {
    offices.get(officeId);
    return counters.findAllByOfficePublicIdAndActiveTrueOrderByCode(officeId).stream()
        .map(this::managementMap)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<OfficerAssignmentHistoryResponse> officerAssignmentHistory(UUID officeId) {
    offices.get(officeId);
    return officerAssignments.findAllByCounterOfficePublicIdOrderByAssignedAtDesc(officeId).stream()
        .map(
            assignment ->
                new OfficerAssignmentHistoryResponse(
                    assignment.getPublicId(),
                    assignment.getOfficer().getPublicId(),
                    assignment.getOfficer().getEmail(),
                    assignment.getCounter().getPublicId(),
                    assignment.getCounter().getCode(),
                    assignment.getAssignedAt(),
                    assignment.getReleasedAt()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<OfficerCounterOptionResponse> operationOptions() {
    return counters.findAllByActiveTrueAndStatusOrderByCode(CounterStatus.OPEN).stream()
        .map(
            counter ->
                new OfficerCounterOptionResponse(
                    counter.getPublicId(),
                    counter.getCode(),
                    counter.getOffice().getName(),
                    counter.getOffice().getCategory(),
                    serviceAssignments
                        .findAllByCounterPublicIdAndActiveTrue(counter.getPublicId())
                        .stream()
                        .map(
                            assignment ->
                                new OfficerCounterOptionResponse.AssignedService(
                                    assignment.getService().getPublicId(),
                                    assignment.getService().getName()))
                        .toList()))
        .filter(option -> !option.services().isEmpty())
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CounterServiceAssignmentResponse> serviceAssignments(UUID officeId) {
    offices.get(officeId);
    return serviceAssignments.findAllByCounterOfficePublicIdAndActiveTrue(officeId).stream()
        .map(
            assignment ->
                new CounterServiceAssignmentResponse(
                    assignment.getCounter().getPublicId(),
                    assignment.getCounter().getCode(),
                    assignment.getService().getPublicId(),
                    assignment.getService().getName(),
                    assignment.getService().getDepartment().getName()))
        .toList();
  }

  @Transactional
  public void assignOfficer(OfficerAssignmentRequest r) {
    Counter c = get(r.counterId());
    UserAccount u =
        users
            .findByPublicId(r.officerId())
            .orElseThrow(() -> new ResourceNotFoundException("Officer", r.officerId()));
    if (!u.isEnabled())
      throw new BusinessConflictException("Disabled officers cannot be assigned to a counter");
    if (u.getRole().getName() != RoleName.OFFICER)
      throw new BusinessConflictException("Only an OFFICER can be assigned to a counter");
    if (officerAssignments.existsByCounterIdAndReleasedAtIsNull(c.getId()))
      throw new BusinessConflictException("Counter already has an active officer assignment");
    if (officerAssignments.existsByOfficerIdAndReleasedAtIsNull(u.getId()))
      throw new BusinessConflictException(
          "Officer is already assigned. Release the officer first to reassign them.");
    officerAssignments.save(new OfficerCounterAssignment(u, c));
  }

  @Transactional
  public void releaseOfficer(UUID counterId) {
    OfficerCounterAssignment a =
        officerAssignments
            .findByCounterPublicIdAndReleasedAtIsNull(counterId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Active officer assignment", counterId));
    a.release();
  }

  @Transactional
  public void assignService(CounterServiceAssignmentRequest r) {
    Counter c = get(r.counterId());
    var s = services.get(r.serviceId());
    if (!s.getDepartment().getOffice().getPublicId().equals(c.getOffice().getPublicId()))
      throw new BusinessConflictException("Counter and service must belong to the same office");
    CounterServiceAssignment a =
        serviceAssignments
            .findByCounterPublicIdAndServicePublicId(r.counterId(), r.serviceId())
            .orElse(null);
    if (a != null && a.isActive())
      throw new BusinessConflictException("Service is already assigned to this counter");
    if (a != null) {
      a.reactivate();
      return;
    }
    serviceAssignments.save(new CounterServiceAssignment(c, s));
  }

  @Transactional
  public void releaseService(UUID counterId, UUID serviceId) {
    CounterServiceAssignment a =
        serviceAssignments
            .findByCounterPublicIdAndServicePublicId(counterId, serviceId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Counter service assignment", serviceId));
    if (!a.isActive())
      throw new BusinessConflictException("Service assignment is already released");
    a.release();
  }

  public Counter get(UUID id) {
    return counters
        .findByPublicIdAndActiveTrue(id)
        .orElseThrow(() -> new ResourceNotFoundException("Counter", id));
  }

  private CounterResponse map(Counter c) {
    return new CounterResponse(
        c.getPublicId(), c.getOffice().getPublicId(), c.getCode(), c.getStatus(), c.isActive());
  }

  private CounterManagementResponse managementMap(Counter c) {
    var officer =
        officerAssignments
            .findByCounterPublicIdAndReleasedAtIsNull(c.getPublicId())
            .map(
                a ->
                    new CounterManagementResponse.AssignedOfficer(
                        a.getOfficer().getPublicId(), a.getOfficer().getEmail()))
            .orElse(null);
    var assignedServices =
        serviceAssignments.findAllByCounterPublicIdAndActiveTrue(c.getPublicId()).stream()
            .map(
                a ->
                    new CounterManagementResponse.AssignedService(
                        a.getService().getPublicId(),
                        a.getService().getName(),
                        a.getService().getDepartment().getName()))
            .toList();
    return new CounterManagementResponse(
        c.getPublicId(),
        c.getOffice().getPublicId(),
        c.getCode(),
        c.getStatus(),
        c.isActive(),
        officer,
        assignedServices);
  }

  private String normalizeCode(String code) {
    return code.trim().toUpperCase(Locale.ROOT);
  }
}
