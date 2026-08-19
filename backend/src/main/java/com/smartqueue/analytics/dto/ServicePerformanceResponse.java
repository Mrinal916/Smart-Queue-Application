package com.smartqueue.analytics.dto;

public record ServicePerformanceResponse(
    Long serviceId, long dailyVolume, double averageWaitMinutes, double averageServiceMinutes) {}
