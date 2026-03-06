package com.applicationplanner.api.controller;

import com.applicationplanner.api.dto.requestDTO.CreateAssignmentRequest;
import com.applicationplanner.api.dto.requestDTO.UpdateAssignmentRequest;
import com.applicationplanner.api.model.Assignment;
import com.applicationplanner.api.service.PlanningOrchestratorService;
import com.applicationplanner.api.util.TimezoneResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/assignments")
public class AssignmentController {

    private final PlanningOrchestratorService orchestrator;
    private final TimezoneResolver timezoneResolver;

    public AssignmentController(
            PlanningOrchestratorService orchestrator,
            TimezoneResolver timezoneResolver) {
        this.orchestrator = orchestrator;
        this.timezoneResolver = timezoneResolver;
    }

    /**
     * Creates a new assignment and generates a plan for it. by Claude
     */
    @PostMapping
    public Assignment create(@Valid @RequestBody CreateAssignmentRequest req) {
        if (req.startDate() != null && !req.startDate().isBefore(req.dueDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before dueDate");
        }

        LocalDate today = timezoneResolver.resolveToday();
        Assignment a = new Assignment();
        a.setTitle(req.title().trim());
        a.setSubject(req.subject().trim());
        a.setDueDate(req.dueDate());
        a.setStartDate(req.startDate());
        return orchestrator.createAssignmentAndPlan(a, today);
    }

    /**
     * Updates an existing assignment and replans. by Claude
     */
    @PatchMapping("/{assignmentId}")
    public void update(
            @PathVariable UUID assignmentId,
            @RequestBody UpdateAssignmentRequest req) {

        if (req.startDate() != null && req.dueDate() != null &&
                !req.startDate().isBefore(req.dueDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before dueDate");
        }

        LocalDate today = timezoneResolver.resolveToday();
        Assignment patch = new Assignment();

        if (req.title() != null) {
            String t = req.title().trim();
            if (t.isEmpty()) throw new IllegalArgumentException("title cannot be blank");
            patch.setTitle(t);
        }
        if (req.subject() != null) {
            String s = req.subject().trim();
            if (s.isEmpty()) throw new IllegalArgumentException("Subject cannot be blank");
            patch.setSubject(s);
        }
        if (req.startDate() != null) patch.setStartDate(req.startDate());
        if (req.dueDate() != null) patch.setDueDate(req.dueDate());

        orchestrator.updateAssignmentAndPlan(assignmentId, patch, today);
    }

    /**
     * Deletes an assignment and replans remaining assignments. by Claude
     */
    @DeleteMapping("/{assignmentId}")
    public void delete(@PathVariable UUID assignmentId) {
        LocalDate today = timezoneResolver.resolveToday();
        orchestrator.removeAssignment(assignmentId, today);
    }
}