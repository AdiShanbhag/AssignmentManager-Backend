package com.applicationplanner.api.dto.requestDTO;

import jakarta.validation.constraints.NotBlank;

// By Claude - Request body for unregistering a device token (Story 11.2)
public record UnregisterTokenRequest(
        @NotBlank(message = "Token is required")
        String token
) {}