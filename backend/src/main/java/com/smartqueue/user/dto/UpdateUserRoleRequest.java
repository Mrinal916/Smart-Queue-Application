package com.smartqueue.user.dto;

import com.smartqueue.user.enums.RoleName;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(@NotNull RoleName role) {}
