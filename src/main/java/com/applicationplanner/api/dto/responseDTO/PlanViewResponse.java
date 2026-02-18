package com.applicationplanner.api.dto.responseDTO;

import com.applicationplanner.api.enums.PanicStatus;

import java.util.List;
import java.util.UUID;

public record PlanViewResponse(
        UUID assignmentId,
        String title,
        String subject,
        String dueDate,          // YYYY-MM-DD
        int planningDays,
        PanicStatus panicStatus,
        List<TaskViewResponse> tasks,
        int hoursConsumedByEarlierAssignments
) {}