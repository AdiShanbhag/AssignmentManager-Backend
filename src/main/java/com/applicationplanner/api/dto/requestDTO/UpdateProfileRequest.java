package com.applicationplanner.api.dto.requestDTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 100, message = "Display name must be between 1 and 100 characters")
        String displayName,

        @Size(max = 20, message = "Phone number must be at most 20 characters")
        String phoneNumber,

        @Size(max = 255, message = "University name must be at most 255 characters")
        String university,

        @Size(max = 100, message = "Timezone must be at most 100 characters")
        String timezone,

        Boolean notificationsEnabled,

        Boolean dailyReminderEnabled,

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Daily reminder time must be in HH:mm format")
        String dailyReminderTime,

        Boolean dueDateWarningEnabled,

        @Min(value = 1, message = "Due date warning must be at least 1 day before")
        @Max(value = 30, message = "Due date warning must be at most 30 days before")
        Integer dueDateWarningDaysBefore,

        Boolean atRiskAlertEnabled
) {}