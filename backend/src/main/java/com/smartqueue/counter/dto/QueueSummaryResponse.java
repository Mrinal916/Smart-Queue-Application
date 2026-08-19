package com.smartqueue.counter.dto;

import com.smartqueue.token.dto.TokenResponse;
import java.util.List;
import java.util.UUID;

public record QueueSummaryResponse(
    UUID counterId,
    TokenResponse currentToken,
    int waitingCount,
    List<TokenResponse> waitingTokens) {}
