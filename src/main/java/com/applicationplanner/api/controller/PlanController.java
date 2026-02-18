package com.applicationplanner.api.controller;

import com.applicationplanner.api.dto.responseDTO.PlanViewResponse;
import com.applicationplanner.api.dto.responseDTO.TaskViewResponse;
import com.applicationplanner.api.record.AssignmentPlanView;
import com.applicationplanner.api.service.PlanningOrchestratorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
public class PlanController {

    private final PlanningOrchestratorService orchestrator;

    public PlanController(PlanningOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @GetMapping("/plan")
    public List<PlanViewResponse> getPlan(
            @RequestParam(required = false) String today
    ) {
        LocalDate effectiveToday =
                (today != null) ? LocalDate.parse(today) : LocalDate.now();

        List<AssignmentPlanView> views = orchestrator.getPlanView(effectiveToday);

        // Optional: keep UI predictable. Frontend stored newest first on add, but global planning sorts by dueDate.
        // Here we sort by dueDate to make the response stable.
        return views.stream()
                .sorted(Comparator.comparing(v -> v.assignment().getDueDate()))
                .map(v -> new PlanViewResponse(
                        v.assignment().getId(),
                        v.assignment().getTitle(),
                        v.assignment().getSubject(),
                        v.assignment().getDueDate().toString(),
                        v.assignment().getPlanningDays(),
                        v.panicStatus(),
                        v.tasks().stream()
                                .sorted(Comparator.comparingInt(t -> t.getOrderIndex()))
                                .map(t -> new TaskViewResponse(
                                        t.getId(),
                                        t.getAssignmentId(),
                                        t.getTitle(),
                                        t.isDone(),
                                        t.getTargetDate() == null ? "" : t.getTargetDate().toString(),
                                        t.getEffortHours(),
                                        t.getOrderIndex(),
                                        t.isUnscheduled(),
                                        t.getUnscheduledReason()
                                ))
                                .toList(),
                        v.hoursConsumedByEarlierAssignments()
                ))
                .toList();
    }
}