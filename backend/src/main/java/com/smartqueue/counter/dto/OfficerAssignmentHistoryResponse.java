package com.smartqueue.counter.dto;

import java.time.Instant;
import java.util.UUID;

public record OfficerAssignmentHistoryResponse(
    UUID assignmentId,
    UUID officerId,
    String officerEmail,
    UUID counterId,
    String counterCode,
    Instant assignedAt,
    Instant releasedAt) {}
