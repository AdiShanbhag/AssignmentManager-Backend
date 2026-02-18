package com.applicationplanner.api.service;

import com.applicationplanner.api.enums.PanicStatus;
import com.applicationplanner.api.model.Assignment;
import com.applicationplanner.api.model.Availability;
import com.applicationplanner.api.model.Task;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PlannerService {

    // Mirrors generateTasks(title, dueDate, assignmentId)
    List<Task> generateDefaultTasks(String assignmentTitle, LocalDate dueDate, UUID assignmentId, LocalDate today);

    // Mirrors buildGlobalPlan(assignments, tasksById, availability)
    Map<UUID, List<Task>> buildGlobalPlan(
            List<Assignment> assignments,
            Map<UUID, List<Task>> tasksByAssignmentId,
            Availability availability,
            LocalDate today
    );

    // Mirrors applyMissedTaskShift(tasks)
    List<Task> applyMissedTaskShift(List<Task> tasks, LocalDate today);

    // Mirrors computePanicStatusV2(assignment, tasks)
    PanicStatus computePanicStatusV2(Assignment assignment, List<Task> tasks, LocalDate today);
}