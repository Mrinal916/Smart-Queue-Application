package com.smartqueue.analytics.dto;

public record OfficeAnalyticsResponse(
    Long officeId, long dailyFootfall, long activeCounters, double completionRate) {}
