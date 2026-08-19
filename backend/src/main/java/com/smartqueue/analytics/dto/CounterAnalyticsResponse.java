package com.smartqueue.analytics.dto;

public record CounterAnalyticsResponse(
    Long counterId,
    long completedTokens,
    double averageProcessingMinutes,
    double utilizationPercent,
    double idleMinutes) {}
