package com.smartqueue.counter.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CounterServiceAssignmentRequest(@NotNull UUID counterId, @NotNull UUID serviceId) {}
