package com.smartqueue.department.dto;

import java.util.UUID;

public record DepartmentResponse(UUID publicId, UUID officeId, String name, boolean active) {}
