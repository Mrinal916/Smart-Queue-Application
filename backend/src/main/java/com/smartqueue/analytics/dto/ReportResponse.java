package com.smartqueue.analytics.dto;

import java.time.LocalDate;

public record ReportResponse(
    LocalDate from,
    LocalDate to,
    long bookings,
    long waiting,
    long completed,
    long cancelled,
    long noShows,
    double cancellationRate,
    double noShowRate,
    double completionRate) {}
