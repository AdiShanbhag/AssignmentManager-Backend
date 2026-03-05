package com.applicationplanner.api.dto.responseDTO;

import java.time.OffsetDateTime;
import java.util.UUID;

// By Claude - Added notification preference fields for Story 11.1
public record UserProfileResponse(
        UUID id,
        String email,
        String displayName,
        String phoneNumber,
        String university,
        String timezone,
        OffsetDateTime createdAt,
        boolean notificationsEnabled,
        boolean dailyReminderEnabled,
        String dailyReminderTime,
        boolean dueDateWarningEnabled,
        int dueDateWarningDaysBefore,
        boolean atRiskAlertEnabled
) {}