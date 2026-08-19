package com.smartqueue.websocket.dto;

import java.time.Instant;
import java.util.UUID;

public record QueueUpdateEvent(
    String type,
    UUID officeId,
    UUID serviceId,
    UUID tokenId,
    UUID citizenId,
    String tokenStatus,
    Instant occurredAt) {}
