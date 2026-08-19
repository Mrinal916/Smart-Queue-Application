package com.smartqueue.counter.repository;

import com.smartqueue.counter.entity.OfficerCounterAssignment;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface OfficerCounterAssignmentRepository
    extends JpaRepository<OfficerCounterAssignment, Long> {
  boolean existsByOfficerIdAndReleasedAtIsNull(Long officerId);

  boolean existsByCounterIdAndReleasedAtIsNull(Long counterId);

  Optional<OfficerCounterAssignment> findByCounterPublicIdAndReleasedAtIsNull(UUID counterId);

  List<OfficerCounterAssignment> findAllByOfficerPublicIdAndReleasedAtIsNull(UUID officerId);

  List<OfficerCounterAssignment> findAllByCounterOfficePublicIdOrderByAssignedAtDesc(UUID officeId);

  boolean existsByOfficerPublicIdAndCounterPublicIdAndReleasedAtIsNull(
      UUID officerId, UUID counterId);

  void deleteAllByOfficerId(Long officerId);
}
