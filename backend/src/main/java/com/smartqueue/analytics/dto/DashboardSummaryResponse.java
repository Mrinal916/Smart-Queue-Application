package com.smartqueue.analytics.dto;

public record DashboardSummaryResponse(
    long activeOffices,
    long activeCounters,
    long activeQueues,
    long activeOfficers,
    long totalCitizens,
    long totalBookingsToday,
    long tokensWaiting,
    long tokensCompleted,
    long tokensCancelled,
    long noShows) {}
