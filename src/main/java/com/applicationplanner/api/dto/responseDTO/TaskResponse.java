package com.applicationplanner.api.dto.responseDTO;

import java.time.LocalDate;
import java.util.UUID;

public class TaskResponse {
    public UUID id;
    public String title;
    public boolean isDone;
    public int effortHours;
    public int orderIndex;

    public LocalDate targetDate;     // null if unscheduled
    public boolean isUnscheduled;
}
