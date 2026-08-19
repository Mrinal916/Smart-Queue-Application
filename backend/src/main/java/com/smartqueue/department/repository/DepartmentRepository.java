package com.smartqueue.department.repository;

import com.smartqueue.department.entity.Department;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
  Optional<Department> findByPublicIdAndActiveTrue(UUID id);

  List<Department> findAllByOfficePublicIdAndActiveTrueOrderByName(UUID officeId);

  boolean existsByOfficePublicIdAndActiveTrue(UUID officeId);
}
