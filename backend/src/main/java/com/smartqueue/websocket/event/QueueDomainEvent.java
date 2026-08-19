package com.smartqueue.websocket.event;

import java.time.Instant;
import java.util.UUID;

public record QueueDomainEvent(
    String type,
    UUID officeId,
    UUID serviceId,
    UUID tokenId,
    UUID citizenId,
    String tokenStatus,
    Instant occurredAt) {}
