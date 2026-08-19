package com.smartqueue.counter.dto;

import java.util.UUID;

public record CounterServiceAssignmentResponse(
    UUID counterId,
    String counterCode,
    UUID serviceId,
    String serviceName,
    String departmentName) {}
