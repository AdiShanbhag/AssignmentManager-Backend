package com.applicationplanner.api.service;

import com.applicationplanner.api.model.Assignment;
import com.applicationplanner.api.model.Availability;
import com.applicationplanner.api.model.Task;
import com.applicationplanner.api.record.AssignmentPlanView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PlanningOrchestratorService {

    Assignment createAssignmentAndPlan(Assignment assignmentInput, LocalDate today);

    void removeAssignment(UUID assignmentId, LocalDate today);

    void updateAssignmentAndPlan(UUID assignmentId, Assignment patch, LocalDate today);

    Availability getAvailabilityOrDefault();

    void setAvailabilityAndPlan(Availability nextAvailability, LocalDate today);

    void updateTaskEffortAndPlan(UUID assignmentId, UUID taskId, int effortHours, LocalDate today);

    void updateTaskTitleAndPlan(UUID assignmentId, UUID taskId, String title, LocalDate today);

    void toggleTaskDoneAndPlan(UUID assignmentId, UUID taskId, LocalDate today);

    // Optional: manual trigger (useful for debugging)
    void recomputePlan(LocalDate today);

    // Optional: if UI wants computed statuses in one call
    List<AssignmentPlanView> getPlanView(LocalDate today);
}