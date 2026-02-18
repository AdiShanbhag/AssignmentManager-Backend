package com.applicationplanner.api.service;

import com.applicationplanner.api.enums.PanicStatus;
import com.applicationplanner.api.model.Assignment;
import com.applicationplanner.api.model.Availability;
import com.applicationplanner.api.model.Task;
import com.applicationplanner.api.record.AssignmentPlanView;
import com.applicationplanner.api.repository.AssignmentRepository;
import com.applicationplanner.api.repository.TaskRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PlanningOrchestratorDueDateTest {

    @Autowired
    private PlanningOrchestratorService orchestrator;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void whenDueDateMovedEarlier_tasksBeyondNewWindowAreUnscheduled() {
        LocalDate today = LocalDate.of(2026, 2, 7);

        Assignment assignment = new Assignment();
        assignment.setTitle("Test");
        assignment.setSubject("Math");
        assignment.setDueDate(LocalDate.of(2026, 2, 15));

        Assignment saved = orchestrator.createAssignmentAndPlan(assignment, today);

        Assignment patch = new Assignment();
        patch.setDueDate(LocalDate.of(2026, 2, 10));

        orchestrator.updateAssignmentAndPlan(saved.getId(), patch, today);

        List<AssignmentPlanView> after = orchestrator.getPlanView(today);
        List<Task> tasks = after.get(0).tasks();

        LocalDate newWorkEnd = LocalDate.of(2026, 2, 9);

        long scheduledCount = tasks.stream()
                .filter(t -> !t.isDone() && t.getTargetDate() != null)
                .count();

        assertTrue(scheduledCount > 0, "Tasks should still be scheduled if capacity allows");

        PanicStatus status = after.get(0).panicStatus();
        assertNotNull(status);

        assertTrue(
                status == PanicStatus.AT_RISK || status == PanicStatus.ON_TRACK
        );

        for (Task t : tasks) {
            if (!t.isDone() && t.getTargetDate() != null) {
                assertFalse(t.getTargetDate().isAfter(newWorkEnd));
            }
        }
    }

    @Test
    void whenAvailabilityIsZeroForADay_noTaskIsScheduledOnThatDay() {
        LocalDate today = LocalDate.of(2026, 2, 9); // Monday

        // Explicit availability
        Availability availability = new Availability();
        availability.setMonHours(0);  // zero capacity
        availability.setTueHours(4);
        availability.setWedHours(4);
        availability.setThuHours(4);
        availability.setFriHours(4);
        availability.setSatHours(4);
        availability.setSunHours(4);

        orchestrator.setAvailabilityAndPlan(availability, today);

        Assignment assignment = new Assignment();
        assignment.setTitle("Zero Monday Test");
        assignment.setSubject("Math");
        assignment.setDueDate(LocalDate.of(2026, 2, 15)); // Sunday

        orchestrator.createAssignmentAndPlan(assignment, today);

        List<AssignmentPlanView> plan = orchestrator.getPlanView(today);
        List<Task> tasks = plan.get(0).tasks();

        for (Task t : tasks) {
            if (!t.isDone() && t.getTargetDate() != null) {
                assertNotEquals(
                        today,
                        t.getTargetDate(),
                        "Task should not be scheduled on zero-capacity day"
                );
            }
        }
    }

    @Test
    void whenZeroAvailabilityInMiddle_schedulerSkipsThatDay() {
        LocalDate today = LocalDate.of(2026, 2, 9); // Monday

        Availability availability = new Availability();
        availability.setMonHours(4);
        availability.setTueHours(0);  // blocked
        availability.setWedHours(4);
        availability.setThuHours(4);
        availability.setFriHours(4);
        availability.setSatHours(4);
        availability.setSunHours(4);

        orchestrator.setAvailabilityAndPlan(availability, today);

        Assignment assignment = new Assignment();
        assignment.setTitle("Middle Zero Test");
        assignment.setSubject("Math");
        assignment.setDueDate(LocalDate.of(2026, 2, 15));

        Assignment saved = orchestrator.createAssignmentAndPlan(assignment, today);

        // Increase first task effort to force multi-day scheduling
        List<Task> tasks = taskRepository.findByAssignmentId(saved.getId());
        Task first = tasks.get(0);
        first.setEffortHours(6);
        taskRepository.save(first);

        orchestrator.recomputePlan(today);

        List<AssignmentPlanView> plan = orchestrator.getPlanView(today);
        List<Task> updatedTasks = plan.get(0).tasks();

        LocalDate blockedDay = today.plusDays(1); // Tuesday

        for (Task t : updatedTasks) {
            if (!t.isDone() && t.getTargetDate() != null) {
                assertNotEquals(
                        blockedDay,
                        t.getTargetDate(),
                        "Task should not be scheduled on zero-capacity day"
                );
            }
        }
    }

    @Test
    void whenEarlierAssignmentDeleted_laterAssignmentReclaimsCapacity() {
        LocalDate today = LocalDate.of(2026, 2, 9); // Monday

        Availability availability = new Availability();
        availability.setMonHours(4);
        availability.setTueHours(4);
        availability.setWedHours(4);
        availability.setThuHours(4);
        availability.setFriHours(4);
        availability.setSatHours(4);
        availability.setSunHours(4);

        orchestrator.setAvailabilityAndPlan(availability, today);

        // Assignment A (earlier due)
        Assignment a = new Assignment();
        a.setTitle("A");
        a.setSubject("Math");
        a.setDueDate(LocalDate.of(2026, 2, 12));

        Assignment savedA = orchestrator.createAssignmentAndPlan(a, today);

        // Assignment B (later due)
        Assignment b = new Assignment();
        b.setTitle("B");
        b.setSubject("Science");
        b.setDueDate(LocalDate.of(2026, 2, 16));

        Assignment savedB = orchestrator.createAssignmentAndPlan(b, today);

        List<AssignmentPlanView> before = orchestrator.getPlanView(today);

        AssignmentPlanView bBefore = before.stream()
                .filter(v -> v.assignment().getId().equals(savedB.getId()))
                .findFirst()
                .orElseThrow();

        int consumedBefore = bBefore.hoursConsumedByEarlierAssignments();
        assertTrue(consumedBefore > 0, "B should have consumed hours from A");

        // Now delete A
        orchestrator.removeAssignment(savedA.getId(), today);

        List<AssignmentPlanView> after = orchestrator.getPlanView(today);

        AssignmentPlanView bAfter = after.stream()
                .filter(v -> v.assignment().getId().equals(savedB.getId()))
                .findFirst()
                .orElseThrow();

        int consumedAfter = bAfter.hoursConsumedByEarlierAssignments();
        assertEquals(0, consumedAfter, "B should reclaim capacity after A is deleted");
    }
}
