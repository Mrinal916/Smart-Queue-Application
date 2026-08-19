package com.smartqueue.counter.dto;

import com.smartqueue.counter.enums.CounterStatus;
import java.util.List;
import java.util.UUID;

public record CounterManagementResponse(
    UUID publicId,
    UUID officeId,
    String code,
    CounterStatus status,
    boolean active,
    AssignedOfficer officer,
    List<AssignedService> services) {
  public record AssignedOfficer(UUID publicId, String email) {}

  public record AssignedService(UUID publicId, String name, String departmentName) {}
}
