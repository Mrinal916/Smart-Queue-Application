package com.smartqueue.counter.repository;

import com.smartqueue.counter.entity.Counter;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounterRepository extends JpaRepository<Counter, Long> {
  Optional<Counter> findByPublicIdAndActiveTrue(UUID id);

  List<Counter> findAllByOfficePublicIdAndActiveTrueOrderByCode(UUID officeId);

  List<Counter> findAllByActiveTrueAndStatusOrderByCode(
      com.smartqueue.counter.enums.CounterStatus status);

  boolean existsByOfficeIdAndCodeIgnoreCase(Long officeId, String code);

  boolean existsByOfficeIdAndCodeIgnoreCaseAndPublicIdNot(
      Long officeId, String code, UUID publicId);
}
