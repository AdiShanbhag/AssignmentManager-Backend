package com.applicationplanner.api.controller;

import com.applicationplanner.api.dto.requestDTO.UpdateTaskEffortHourRequest;
import com.applicationplanner.api.dto.requestDTO.UpdateTaskTitleRequest;
import com.applicationplanner.api.service.PlanningOrchestratorService;
import com.applicationplanner.api.util.TimezoneResolver;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final PlanningOrchestratorService orchestrator;
    private final TimezoneResolver timezoneResolver;

    public TaskController(PlanningOrchestratorService orchestrator,
                          TimezoneResolver timezoneResolver) {
        this.orchestrator = orchestrator;
        this.timezoneResolver = timezoneResolver;
    }

    @PatchMapping("/{taskId}/toggle")
    public void toggleTask(@PathVariable UUID taskId,
                           @RequestParam UUID assignmentId) {
        LocalDate today = timezoneResolver.resolveToday();
        orchestrator.toggleTaskDoneAndPlan(assignmentId, taskId, today);
    }

    @PatchMapping("/{taskId}/effort")
    public void updateEffort(
            @PathVariable UUID taskId,
            @RequestParam UUID assignmentId,
            @Valid @RequestBody UpdateTaskEffortHourRequest req
    ) {
        LocalDate today = timezoneResolver.resolveToday();
        orchestrator.updateTaskEffortAndPlan(assignmentId, taskId, req.effortHours(), today);
    }

    @PatchMapping("/{taskId}/title")
    public void updateTitle(
            @PathVariable UUID taskId,
            @RequestParam UUID assignmentId,
            @Valid @RequestBody UpdateTaskTitleRequest req
    ) {
        LocalDate today = timezoneResolver.resolveToday();
        orchestrator.updateTaskTitleAndPlan(assignmentId, taskId, req.title(), today);
    }
}