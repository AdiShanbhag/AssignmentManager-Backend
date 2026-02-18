package com.applicationplanner.api.dto.responseDTO;

import com.applicationplanner.api.enums.UnscheduledReason;

import java.util.UUID;

public record TaskViewResponse(
        UUID id,
        UUID assignmentId,
        String title,
        boolean isDone,
        String targetDate,       // "" if null
        int effortHours,
        int orderIndex,
        boolean isUnscheduled,
        UnscheduledReason unscheduledReason
) {}
