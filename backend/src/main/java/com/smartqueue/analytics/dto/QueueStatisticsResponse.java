package com.smartqueue.analytics.dto;

import java.time.DayOfWeek;

public record QueueStatisticsResponse(
    double averageWaitMinutes,
    double averageServiceMinutes,
    double averageQueueLength,
    Integer peakHour,
    DayOfWeek peakDay,
    double queueThroughput,
    double cancellationRate,
    double noShowRate,
    double serviceCompletionRate) {}
