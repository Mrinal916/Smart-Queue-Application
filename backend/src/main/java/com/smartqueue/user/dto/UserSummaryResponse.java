package com.smartqueue.user.dto;

import com.smartqueue.user.enums.RoleName;
import java.util.UUID;

public record UserSummaryResponse(UUID publicId, String email, RoleName role, boolean enabled) {}
