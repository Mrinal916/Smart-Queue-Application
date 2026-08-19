package com.smartqueue.counter.dto;

import com.smartqueue.counter.enums.CounterStatus;
import java.util.UUID;

public record CounterResponse(
    UUID publicId, UUID officeId, String code, CounterStatus status, boolean active) {}
