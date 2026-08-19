package com.smartqueue.department.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record DepartmentRequest(@NotNull UUID officeId, @NotBlank @Size(max = 150) String name) {}
