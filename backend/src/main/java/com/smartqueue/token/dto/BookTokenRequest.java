package com.smartqueue.token.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookTokenRequest(
    @NotBlank @Size(max = 150) String visitorName,
    @NotBlank @Pattern(regexp = "^[0-9+() -]{7,30}$", message = "Enter a valid phone number")
        String visitorPhone,
    @NotNull @Min(0) @Max(130) Integer visitorAge,
    @NotBlank @Pattern(regexp = "MALE|FEMALE|OTHER|PREFER_NOT_TO_SAY") String visitorGender,
    @NotNull UUID serviceId,
    @NotNull @FutureOrPresent LocalDate appointmentDate,
    @NotNull LocalTime appointmentTime,
    @NotBlank @Size(max = 100) String idempotencyKey) {}
