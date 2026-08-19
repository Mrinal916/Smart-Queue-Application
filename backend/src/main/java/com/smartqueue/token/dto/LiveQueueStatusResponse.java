package com.smartqueue.token.dto;

import java.util.UUID;

public record LiveQueueStatusResponse(
    UUID serviceId, TokenResponse currentServing, int waitingCount) {}
