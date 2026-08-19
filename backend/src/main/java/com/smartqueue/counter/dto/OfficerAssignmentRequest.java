package com.smartqueue.counter.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OfficerAssignmentRequest(@NotNull UUID officerId, @NotNull UUID counterId) {}
