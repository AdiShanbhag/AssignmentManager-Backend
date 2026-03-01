package com.applicationplanner.api.controller;

import com.applicationplanner.api.dto.requestDTO.CreateAssignmentRequest;
import com.applicationplanner.api.dto.requestDTO.UpdateAssignmentRequest;
import com.applicationplanner.api.model.Assignment;
import com.applicationplanner.api.service.PlanningOrchestratorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

import static com.applicationplanner.api.util.TimezoneUtil.resolveToday;

@RestController
@RequestMapping("/assignments")
public class AssignmentController {

    private final PlanningOrchestratorService orchestrator;

    public AssignmentController(PlanningOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping
    public Assignment create(
            @Valid @RequestBody CreateAssignmentRequest req,
            @RequestParam(required = false) String tz) {
        LocalDate effectiveToday = resolveToday(tz);
        Assignment a = new Assignment();
        a.setTitle(req.title().trim());
        a.setSubject(req.subject().trim());
        a.setDueDate(req.dueDate());
        return orchestrator.createAssignmentAndPlan(a, effectiveToday);
    }

    @PatchMapping("/{assignmentId}")
    public void update(
            @PathVariable UUID assignmentId,
            @RequestBody UpdateAssignmentRequest req,
            @RequestParam(required = false) String tz) {
        LocalDate effectiveToday = resolveToday(tz);
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
        if (req.dueDate() != null) patch.setDueDate(req.dueDate());

        orchestrator.updateAssignmentAndPlan(assignmentId, patch, effectiveToday);
    }

    @DeleteMapping("/{assignmentId}")
    public void delete(
            @PathVariable UUID assignmentId,
            @RequestParam(required = false) String tz) {
        LocalDate effectiveToday = resolveToday(tz);
        orchestrator.removeAssignment(assignmentId, effectiveToday);
    }
}