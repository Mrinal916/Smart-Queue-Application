package com.smartqueue.token.dto;

import com.smartqueue.token.enums.TokenStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TokenResponse(
    UUID publicId,
    int tokenNumber,
    LocalDate queueDate,
    LocalTime appointmentTime,
    TokenStatus status,
    UUID serviceId,
    UUID officeId,
    String officeName,
    String officeAddress,
    String departmentName,
    String serviceName,
    String visitorName,
    String visitorPhone,
    Integer visitorAge,
    String visitorGender,
    boolean agePriority,
    boolean appeared,
    Instant appearedAt,
    UUID counterId,
    String counterCode) {}
