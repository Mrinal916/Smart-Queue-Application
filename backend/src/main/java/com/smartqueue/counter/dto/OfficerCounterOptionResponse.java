package com.smartqueue.counter.dto;

import java.util.List;
import java.util.UUID;

public record OfficerCounterOptionResponse(
    UUID counterId,
    String counterCode,
    String officeName,
    String officeCategory,
    List<AssignedService> services) {

  public record AssignedService(UUID serviceId, String serviceName) {}
}
