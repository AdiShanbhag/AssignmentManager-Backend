package com.applicationplanner.api.dto.requestDTO;

import jakarta.validation.constraints.NotBlank;

// By Claude - Request body for registering a device token (Story 11.2)
public record RegisterTokenRequest(
        @NotBlank(message = "Token is required")
        String token,

        @NotBlank(message = "Platform is required")
        String platform
) {}