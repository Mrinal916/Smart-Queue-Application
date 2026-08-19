package com.smartqueue.queue.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CounterOperationRequest(@NotNull UUID counterId, @NotNull UUID serviceId) {}
