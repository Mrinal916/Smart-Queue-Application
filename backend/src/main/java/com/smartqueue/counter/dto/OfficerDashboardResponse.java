package com.smartqueue.counter.dto;

import com.smartqueue.token.dto.TokenResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OfficerDashboardResponse(
    UUID counterId,
    String counterCode,
    String officeName,
    String serviceName,
    String counterStatus,
    LocalDate queueDate,
    TokenResponse currentToken,
    List<TokenResponse> tokens,
    int waitingCount,
    long completedCount,
    long cancelledCount,
    long arrivedCount,
    int averageWaitMinutes) {}
