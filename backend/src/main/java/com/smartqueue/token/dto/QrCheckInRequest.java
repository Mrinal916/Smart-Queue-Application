package com.smartqueue.token.dto;

import jakarta.validation.constraints.NotBlank;

public record QrCheckInRequest(@NotBlank String payload) {}
