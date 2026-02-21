package com.applicationplanner.api.dto.responseDTO;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String email,
        String displayName
) {}
