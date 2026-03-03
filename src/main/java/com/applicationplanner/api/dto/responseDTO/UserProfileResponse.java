// UserProfileResponse.java
package com.applicationplanner.api.dto.responseDTO;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String displayName,
        String phoneNumber,
        String university,
        String timezone,
        OffsetDateTime createdAt
) {}