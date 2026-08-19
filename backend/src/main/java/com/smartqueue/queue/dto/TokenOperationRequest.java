package com.smartqueue.queue.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TokenOperationRequest(@NotNull UUID counterId) {}
