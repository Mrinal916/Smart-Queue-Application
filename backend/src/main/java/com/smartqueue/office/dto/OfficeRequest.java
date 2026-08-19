package com.smartqueue.office.dto;

import jakarta.validation.constraints.*;

public record OfficeRequest(
    @NotBlank @Size(max = 30) String code,
    @NotBlank @Size(max = 150) String name,
    @NotBlank @Size(max = 500) String address,
    @NotBlank @Pattern(regexp = "HOSPITAL|RTO|OTHER") String category) {}
