package com.applicationplanner.api.service;

import com.applicationplanner.api.enums.PanicStatus;
import com.applicationplanner.api.model.Assignment;
import com.applicationplanner.api.model.Task;
import com.applicationplanner.api.record.AssignmentPlanView;
import com.applicationplanner.api.service.impl.PlannerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class PlannerServicePanicStatusTest {

    private PlannerService plannerService;

    @BeforeEach
    void setup() {
        // If PlannerServiceImpl has dependencies, mock them.
        // Ideally it should be pure and new-able.
        plannerService = new PlannerServiceImpl();
    }

    @Test
    void feasible_finishesEarly_shouldBeOnTrack() {
        LocalDate today = LocalDate.of(2026, 2, 7);
        LocalDate due = LocalDate.of(2026, 2, 11); // workEnd = 10th

        Assignment a = assignment(due);

        List<Task> tasks = List.of(
                task(1, 2, LocalDate.of(2026, 2, 8), false, false),
                task(2, 2, LocalDate.of(2026, 2, 9), false, false),
                task(3, 2, LocalDate.of(2026, 2, 9), false, false),
                task(4, 2, LocalDate.of(2026, 2, 9), false, false),
                task(5, 2, LocalDate.of(2026, 2, 9), false, false)
        );

        assertEquals(PanicStatus.ON_TRACK, plannerService.computePanicStatusV2(a, tasks, today));
    }

    @Test
    void feasible_finishesOnWorkEnd_shouldBeAtRisk() {
        LocalDate today = LocalDate.of(2026, 2, 7);
        LocalDate due = LocalDate.of(2026, 2, 11); // workEnd = 10th
        Assignment a = assignment(due);

        List<Task> tasks = List.of(
                task(1, 2, LocalDate.of(2026, 2, 10), false, false)
        );

        assertEquals(PanicStatus.AT_RISK, plannerService.computePanicStatusV2(a, tasks, today));
    }

    @Test
    void anyIncompleteTaskScheduledAfterWorkEnd_shouldBeScrewed() {
        LocalDate today = LocalDate.of(2026, 2, 7);
        LocalDate due = LocalDate.of(2026, 2, 11); // workEnd = 10th
        Assignment a = assignment(due);

        List<Task> tasks = List.of(
                task(1, 2, LocalDate.of(2026, 2, 11), false, false) // after workEnd
        );

        assertEquals(PanicStatus.SCREWED, plannerService.computePanicStatusV2(a, tasks, today));
    }

    @Test
    void hasUnscheduledIncompleteTask_withMoreThan3DaysRemaining_shouldBeAtRisk() {
        // remainingDays = today..workEnd inclusive
        // today=7, workEnd=10 => 4 days => should be AT_RISK (per your current rule)
        LocalDate today = LocalDate.of(2026, 2, 7);
        LocalDate due = LocalDate.of(2026, 2, 11);
        Assignment a = assignment(due);

        List<Task> tasks = List.of(
                task(1, 2, LocalDate.of(2026, 2, 8), false, false),
                task(2, 2, null, false, true) // unscheduled + incomplete
        );

        assertEquals(PanicStatus.AT_RISK, plannerService.computePanicStatusV2(a, tasks, today));
    }

    @Test
    void hasUnscheduledIncompleteTask_with3DaysOrLessRemaining_shouldBeScrewed() {
        // today=8, due=11 => workEnd=10 => remainingDays = 8,9,10 = 3 => SCREWED (per your rule)
        LocalDate today = LocalDate.of(2026, 2, 8);
        LocalDate due = LocalDate.of(2026, 2, 11);
        Assignment a = assignment(due);

        List<Task> tasks = List.of(
                task(1, 2, null, false, true)
        );

        assertEquals(PanicStatus.SCREWED, plannerService.computePanicStatusV2(a, tasks, today));
    }

    @Test
    void todayPastWorkEnd_shouldBeScrewed() {
        LocalDate today = LocalDate.of(2026, 2, 11);
        LocalDate due = LocalDate.of(2026, 2, 11); // workEnd=10, today > 10
        Assignment a = assignment(due);

        List<Task> tasks = List.of(
                task(1, 2, LocalDate.of(2026, 2, 10), false, false)
        );

        assertEquals(PanicStatus.SCREWED, plannerService.computePanicStatusV2(a, tasks, today));
    }

    @Test
    void allTasksDone_shouldBeOnTrack() {
        LocalDate today = LocalDate.of(2026, 2, 7);
        LocalDate due = LocalDate.of(2026, 2, 11);
        Assignment a = assignment(due);

        List<Task> tasks = List.of(
                task(1, 2, LocalDate.of(2026, 2, 8), true, false),
                task(2, 2, LocalDate.of(2026, 2, 9), true, false)
        );

        assertEquals(PanicStatus.ON_TRACK, plannerService.computePanicStatusV2(a, tasks, today));
    }

    // ---------------- helpers ----------------

    private static Assignment assignment(LocalDate dueDate) {
        Assignment a = new Assignment();
        a.setId(UUID.randomUUID());
        a.setDueDate(dueDate);
        a.setTitle("Test");
        a.setSubject("Test");
        return a;
    }

    private static Task task(int orderIndex, int effortHours, LocalDate targetDate, boolean done, boolean unscheduled) {
        Task t = new Task();
        t.setId(UUID.randomUUID());
        t.setOrderIndex(orderIndex);
        t.setEffortHours(effortHours);
        t.setTargetDate(targetDate);
        t.setDone(done);
        t.setUnscheduled(unscheduled);
        // if you store unscheduledReason, set it here too if needed
        return t;
    }
}