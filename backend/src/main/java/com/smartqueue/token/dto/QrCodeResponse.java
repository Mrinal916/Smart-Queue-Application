package com.smartqueue.token.dto;

import java.time.Instant;
import java.util.UUID;

public record QrCodeResponse(UUID tokenId, String payload, Instant expiresAt) {}
