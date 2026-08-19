package com.smartqueue.token.dto;

import java.util.UUID;

public record WaitTimeResponse(UUID tokenId, int peopleAhead, int estimatedMinutes) {}
