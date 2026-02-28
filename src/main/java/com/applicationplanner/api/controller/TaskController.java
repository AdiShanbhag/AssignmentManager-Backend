package com.applicationplanner.api.controller;

import com.applicationplanner.api.dto.requestDTO.UpdateTaskEffortHourRequest;
import com.applicationplanner.api.dto.requestDTO.UpdateTaskTitleRequest;
import com.applicationplanner.api.service.PlanningOrchestratorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

import static com.applicationplanner.api.util.TimezoneUtil.resolveToday;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final PlanningOrchestratorService orchestrator;

    public TaskController(PlanningOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PatchMapping("/{taskId}/toggle")
    public void toggleTask(@PathVariable UUID taskId,
                           @RequestParam UUID assignmentId,
                           @RequestParam(required = false) String tz) {
        LocalDate effectiveToday = resolveToday(tz);
        orchestrator.toggleTaskDoneAndPlan(assignmentId, taskId, effectiveToday);
    }

    @PatchMapping("/{taskId}/effort")
    public void updateEffort(
            @PathVariable UUID taskId,
            @RequestParam UUID assignmentId,
            @Valid @RequestBody UpdateTaskEffortHourRequest req,
            @RequestParam(required = false) String tz
    ) {
        LocalDate effectiveToday = resolveToday(tz);
        orchestrator.updateTaskEffortAndPlan(assignmentId, taskId, req.effortHours(), effectiveToday);
    }

    @PatchMapping("/{taskId}/title")
    public void updateTitle(
            @PathVariable UUID taskId,
            @RequestParam UUID assignmentId,
            @Valid @RequestBody UpdateTaskTitleRequest req,
            @RequestParam(required = false) String tz
    ) {
        LocalDate effectiveToday = resolveToday(tz);
        orchestrator.updateTaskTitleAndPlan(assignmentId, taskId, req.title(), effectiveToday);
    }
}