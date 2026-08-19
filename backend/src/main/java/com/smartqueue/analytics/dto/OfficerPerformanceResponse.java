package com.smartqueue.analytics.dto;

public record OfficerPerformanceResponse(
    Long officerId,
    long tokensServed,
    long skippedTokens,
    long noShows,
    double averageServiceMinutes) {}
