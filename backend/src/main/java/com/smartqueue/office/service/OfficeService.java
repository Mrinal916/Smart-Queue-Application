package com.smartqueue.office.service;

import com.smartqueue.common.exception.*;
import com.smartqueue.counter.entity.CounterServiceAssignment;
import com.smartqueue.counter.entity.OfficerCounterAssignment;
import com.smartqueue.counter.repository.CounterRepository;
import com.smartqueue.counter.repository.CounterServiceAssignmentRepository;
import com.smartqueue.counter.repository.OfficerCounterAssignmentRepository;
import com.smartqueue.department.repository.DepartmentRepository;
import com.smartqueue.office.dto.*;
import com.smartqueue.office.entity.Office;
import com.smartqueue.office.repository.OfficeRepository;
import com.smartqueue.queue.config.RedisQueueProperties;
import com.smartqueue.queue.entity.QueueHistory;
import com.smartqueue.queue.repository.QueueHistoryRepository;
import com.smartqueue.servicecatalog.repository.QueueServiceRepository;
import com.smartqueue.token.enums.TokenStatus;
import com.smartqueue.token.repository.TokenRepository;
import java.util.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeService {
  private final OfficeRepository repository;
  private final DepartmentRepository departments;
  private final QueueServiceRepository services;
  private final CounterRepository counters;
  private final CounterServiceAssignmentRepository serviceAssignments;
  private final OfficerCounterAssignmentRepository officerAssignments;
  private final TokenRepository tokens;
  private final QueueHistoryRepository history;
  private final StringRedisTemplate redis;
  private final RedisQueueProperties redisProperties;

  public OfficeService(
      OfficeRepository repository,
      DepartmentRepository departments,
      QueueServiceRepository services,
      CounterRepository counters,
      CounterServiceAssignmentRepository serviceAssignments,
      OfficerCounterAssignmentRepository officerAssignments,
      TokenRepository tokens,
      QueueHistoryRepository history,
      StringRedisTemplate redis,
      RedisQueueProperties redisProperties) {
    this.repository = repository;
    this.departments = departments;
    this.services = services;
    this.counters = counters;
    this.serviceAssignments = serviceAssignments;
    this.officerAssignments = officerAssignments;
    this.tokens = tokens;
    this.history = history;
    this.redis = redis;
    this.redisProperties = redisProperties;
  }

  @Transactional
  public OfficeResponse create(OfficeRequest r) {
    return map(
        repository.save(
            new Office(r.code().trim(), r.name().trim(), r.address().trim(), r.category())));
  }

  @Transactional
  public OfficeResponse update(UUID id, OfficeRequest r) {
    Office o = get(id);
    o.update(r.code().trim(), r.name().trim(), r.address().trim(), r.category(), true);
    return map(o);
  }

  @Transactional
  public void delete(UUID id) {
    Office office = get(id);
    UUID officeId = office.getPublicId();
    tokens
        .findAllByOfficePublicIdAndStatus(officeId, TokenStatus.WAITING)
        .forEach(
            token -> {
              token.transition(TokenStatus.CANCELLED, null);
              history.save(
                  new QueueHistory(token, TokenStatus.WAITING, TokenStatus.CANCELLED, null, null));
              redis
                  .opsForZSet()
                  .remove(
                      redisProperties.keyPrefix()
                          + ":"
                          + token.getService().getPublicId()
                          + ":"
                          + token.getQueueDate(),
                      token.getPublicId().toString());
            });
    counters
        .findAllByOfficePublicIdAndActiveTrueOrderByCode(officeId)
        .forEach(
            counter -> {
              officerAssignments
                  .findByCounterPublicIdAndReleasedAtIsNull(counter.getPublicId())
                  .ifPresent(OfficerCounterAssignment::release);
              serviceAssignments
                  .findAllByCounterPublicIdAndActiveTrue(counter.getPublicId())
                  .forEach(CounterServiceAssignment::release);
              counter.deactivate();
            });
    services
        .findAllByDepartmentOfficePublicIdAndActiveTrue(officeId)
        .forEach(service -> service.deactivate());
    departments
        .findAllByOfficePublicIdAndActiveTrueOrderByName(officeId)
        .forEach(department -> department.deactivate());
    office.deactivate();
  }

  @Transactional(readOnly = true)
  public OfficeResponse getResponse(UUID id) {
    return map(get(id));
  }

  @Transactional(readOnly = true)
  public List<OfficeResponse> list() {
    return repository.findAllByActiveTrueOrderByName().stream().map(this::map).toList();
  }

  public Office get(UUID id) {
    return repository
        .findByPublicIdAndActiveTrue(id)
        .orElseThrow(() -> new ResourceNotFoundException("Office", id));
  }

  private OfficeResponse map(Office o) {
    return new OfficeResponse(
        o.getPublicId(), o.getCode(), o.getName(), o.getAddress(), o.getCategory(), o.isActive());
  }
}
