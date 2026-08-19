package com.smartqueue.office.repository;

import com.smartqueue.office.entity.Office;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeRepository extends JpaRepository<Office, Long> {
  Optional<Office> findByPublicIdAndActiveTrue(UUID publicId);

  List<Office> findAllByActiveTrueOrderByName();
}
