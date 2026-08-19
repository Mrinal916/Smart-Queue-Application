package com.smartqueue.websocket.event;

import java.time.Instant;
import java.util.UUID;

public record CounterDomainEvent(
    UUID officeId, UUID counterId, String status, Instant occurredAt) {}
