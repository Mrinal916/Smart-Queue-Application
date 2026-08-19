package com.smartqueue.counter.repository;

import com.smartqueue.counter.entity.CounterServiceAssignment;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounterServiceAssignmentRepository
    extends JpaRepository<CounterServiceAssignment, Long> {
  Optional<CounterServiceAssignment> findByCounterPublicIdAndServicePublicId(
      UUID counterId, UUID serviceId);

  Optional<CounterServiceAssignment> findFirstByServicePublicIdAndActiveTrueOrderByCounterCodeAsc(
      UUID serviceId);

  List<CounterServiceAssignment> findAllByCounterPublicIdAndActiveTrue(UUID counterId);

  List<CounterServiceAssignment> findAllByCounterOfficePublicIdAndActiveTrue(UUID officeId);

  boolean existsByCounterPublicIdAndServicePublicIdAndActiveTrue(UUID counterId, UUID serviceId);
}
