package com.smartqueue.websocket.dto;

import java.time.Instant;
import java.util.UUID;

public record CounterStatusEvent(
    UUID officeId, UUID counterId, String status, Instant occurredAt) {}
