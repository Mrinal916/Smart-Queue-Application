package com.smartqueue.servicecatalog.repository;

import com.smartqueue.servicecatalog.entity.QueueService;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface QueueServiceRepository extends JpaRepository<QueueService, Long> {

  Optional<QueueService> findByPublicIdAndActiveTrue(UUID id);

  List<QueueService> findAllByDepartmentPublicIdAndActiveTrueOrderByName(UUID departmentId);

  List<QueueService> findAllByDepartmentOfficePublicIdAndActiveTrue(UUID officeId);

  boolean existsByDepartmentPublicIdAndActiveTrue(UUID departmentId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from QueueService s where s.publicId = :id and s.active = true")
  Optional<QueueService> findLockedByPublicId(UUID id);
}
