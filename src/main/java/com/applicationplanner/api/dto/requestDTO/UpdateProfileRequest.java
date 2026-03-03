// UpdateProfileRequest.java
package com.applicationplanner.api.dto.requestDTO;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 100, message = "Display name must be between 1 and 100 characters")
        String displayName,

        @Size(max = 20, message = "Phone number must be at most 20 characters")
        String phoneNumber,

        @Size(max = 255, message = "University name must be at most 255 characters")
        String university,

        @Size(max = 100, message = "Timezone must be at most 100 characters")
        String timezone
) {}