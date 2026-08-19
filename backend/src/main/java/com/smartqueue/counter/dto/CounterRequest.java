package com.smartqueue.counter.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record CounterRequest(@NotNull UUID officeId, @NotBlank @Size(max = 30) String code) {}
