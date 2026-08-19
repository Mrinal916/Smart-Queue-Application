package com.smartqueue.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank @Size(max = 200) String token,
    @NotBlank
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,72}$",
            message =
                "Password must include uppercase, lowercase, number, and symbol characters, with no"
                    + " spaces")
        String password) {}
