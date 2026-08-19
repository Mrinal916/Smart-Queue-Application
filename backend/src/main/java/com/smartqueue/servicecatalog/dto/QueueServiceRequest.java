package com.smartqueue.servicecatalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record QueueServiceRequest(
    @NotNull UUID departmentId,
    @NotBlank @Size(max = 150) String name,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    LocalTime breakStartTime,
    LocalTime breakEndTime,
    @Positive int dailyCapacity,
    Integer averageServiceMinutes,
    @Size(min = 1) Set<DayOfWeek> openDays) {}
