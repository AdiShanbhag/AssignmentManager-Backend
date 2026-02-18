package com.applicationplanner.api.controller;

import com.applicationplanner.api.dto.requestDTO.CreateAssignmentRequest;
import com.applicationplanner.api.dto.requestDTO.UpdateAssignmentRequest;
import com.applicationplanner.api.model.Assignment;
import com.applicationplanner.api.service.PlanningOrchestratorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/assignments")
public class AssignmentController {

    private final PlanningOrchestratorService orchestrator;

    public AssignmentController(PlanningOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping
    public Assignment create(@Valid @RequestBody CreateAssignmentRequest req, @RequestParam(required = false) String today) {
        LocalDate effectiveToday = resolveToday(today);
        Assignment a = new Assignment();
        a.setTitle(req.title().trim());
        a.setSubject(req.subject().trim());
        a.setDueDate(req.dueDate());
        return orchestrator.createAssignmentAndPlan(a, effectiveToday);
    }

    @PatchMapping("/{assignmentId}")
    public void update(
            @PathVariable java.util.UUID assignmentId,
            @RequestBody UpdateAssignmentRequest req,
            @RequestParam(required = false) String today
    ) {
        LocalDate effectiveToday = resolveToday(today);
        Assignment patch = new Assignment();

        if (req.title() != null) {
            String t = req.title().trim();
            if (t.isEmpty()) throw new IllegalArgumentException("title cannot be blank");
            patch.setTitle(t);
        }
        if (req.subject() != null) {
            String t = req.subject().trim();
            if (t.isEmpty()) throw new IllegalArgumentException("Subject cannot be blank");
            patch.setTitle(t);
        }

        if (req.dueDate() != null) patch.setDueDate(req.dueDate());

        orchestrator.updateAssignmentAndPlan(assignmentId, patch, effectiveToday);
    }

    @DeleteMapping("/{assignmentId}")
    public void delete(@PathVariable java.util.UUID assignmentId, @RequestParam(required = false) String today) {
        LocalDate effectiveToday = resolveToday(today);
        orchestrator.removeAssignment(assignmentId, effectiveToday);
    }

    //Helper for resolving date conflicts between backend and frontend
    private LocalDate resolveToday(String todayParam) {
        return (todayParam != null) ? LocalDate.parse(todayParam) : LocalDate.now();
    }
}