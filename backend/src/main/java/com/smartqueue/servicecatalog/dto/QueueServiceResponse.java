package com.smartqueue.servicecatalog.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record QueueServiceResponse(
    UUID publicId,
    UUID departmentId,
    String name,
    LocalTime startTime,
    LocalTime endTime,
    LocalTime breakStartTime,
    LocalTime breakEndTime,
    int dailyCapacity,
    int averageServiceMinutes,
    Set<DayOfWeek> openDays,
    boolean active) {}
