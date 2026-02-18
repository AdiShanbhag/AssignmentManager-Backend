package com.applicationplanner.api.record;

import com.applicationplanner.api.enums.PanicStatus;
import com.applicationplanner.api.model.Assignment;
import com.applicationplanner.api.model.Task;
import java.util.List;

public record AssignmentPlanView(
        Assignment assignment,
        PanicStatus panicStatus,
        List<Task> tasks,
        int hoursConsumedByEarlierAssignments
) {}
