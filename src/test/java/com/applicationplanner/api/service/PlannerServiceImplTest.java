package com.applicationplanner.api.service;

import com.applicationplanner.api.model.Availability;
import com.applicationplanner.api.model.Task;
import com.applicationplanner.api.service.impl.PlannerServiceImpl;
import org.junit.jupiter.api.Test;

import com.applicationplanner.api.enums.PanicStatus;
import com.applicationplanner.api.model.Assignment;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

class PlannerServiceImplTest {

    private final PlannerServiceImpl planner = new PlannerServiceImpl();
    private final LocalDate TODAY = LocalDate.of(2025, 6, 1); // Sunday

    // --- applyMissedTaskShift ---

    @Test
    void missedShift_shiftsByCorrectDays() {
        LocalDate staleDate = TODAY.minusDays(3);
        Task t = taskWithDate(staleDate);

        List<Task> result = planner.applyMissedTaskShift(List.of(t), TODAY);

        assertEquals(TODAY, result.get(0).getTargetDate());
    }

    @Test
    void missedShift_noShiftWhenUpToDate() {
        Task t = taskWithDate(TODAY.plusDays(1));
        List<Task> result = planner.applyMissedTaskShift(List.of(t), TODAY);

        assertEquals(TODAY.plusDays(1), result.get(0).getTargetDate());
    }

    @Test
    void missedShift_doneTasksNotShifted() {
        Task done = taskWithDate(TODAY.minusDays(5));
        done.setDone(true);

        List<Task> result = planner.applyMissedTaskShift(List.of(done), TODAY);
        assertEquals(TODAY.minusDays(5), result.get(0).getTargetDate());
    }

    // --- computePanicStatusV2 ---

    @Test
    void panic_allDone_isOnTrack() {
        Assignment a = assignmentDue(TODAY.plusDays(5));
        Task t = taskWithDate(TODAY.plusDays(1));
        t.setDone(true);

        assertEquals(PanicStatus.ON_TRACK, planner.computePanicStatusV2(a, List.of(t), TODAY));
    }

    @Test
    void panic_pastWorkEnd_isScrewed() {
        Assignment a = assignmentDue(TODAY.minusDays(1)); // due yesterday, workEnd = 2 days ago
        Task t = taskWithDate(null);

        assertEquals(PanicStatus.SCREWED, planner.computePanicStatusV2(a, List.of(t), TODAY));
    }

    @Test
    void panic_unscheduledWithTightWindow_isScrewed() {
        Assignment a = assignmentDue(TODAY.plusDays(3));
        Task t = taskWithDate(null);
        t.setUnscheduled(true);

        assertEquals(PanicStatus.SCREWED, planner.computePanicStatusV2(a, List.of(t), TODAY));
    }

    // --- buildGlobalPlan ---

    @Test
    void globalPlan_schedulesTaskWithinWindow() {
        Assignment a = assignmentDue(TODAY.plusDays(7));
        a.setId(UUID.randomUUID());

        Task t = new Task();
        t.setId(UUID.randomUUID());
        t.setAssignmentId(a.getId());
        t.setEffortHours(2);
        t.setDone(false);

        Availability avail = uniformAvailability(4);

        // By Claude - Pass effectiveStartDates map with today as start for this assignment
        Map<UUID, LocalDate> effectiveStartDates = Map.of(a.getId(), TODAY);

        Map<UUID, List<Task>> result = planner.buildGlobalPlan(
                List.of(a),
                Map.of(a.getId(), List.of(t)),
                avail,
                TODAY,
                effectiveStartDates
        );

        Task planned = result.get(a.getId()).get(0);
        assertFalse(planned.isUnscheduled());
        assertNotNull(planned.getTargetDate());
    }

    @Test
    void globalPlan_marksUnscheduledWhenNoCapacity() {
        Assignment a = assignmentDue(TODAY.plusDays(3));
        a.setId(UUID.randomUUID());

        Task t = new Task();
        t.setId(UUID.randomUUID());
        t.setAssignmentId(a.getId());
        t.setEffortHours(100);
        t.setDone(false);

        Availability avail = uniformAvailability(1);

        // By Claude - Pass effectiveStartDates map with today as start for this assignment
        Map<UUID, LocalDate> effectiveStartDates = Map.of(a.getId(), TODAY);

        Map<UUID, List<Task>> result = planner.buildGlobalPlan(
                List.of(a),
                Map.of(a.getId(), List.of(t)),
                avail,
                TODAY,
                effectiveStartDates
        );

        assertTrue(result.get(a.getId()).get(0).isUnscheduled());
    }

    // --- Helpers ---

    private Task taskWithDate(LocalDate date) {
        Task t = new Task();
        t.setId(UUID.randomUUID());
        t.setAssignmentId(UUID.randomUUID());
        t.setEffortHours(1);
        t.setDone(false);
        t.setTargetDate(date);
        return t;
    }

    private Assignment assignmentDue(LocalDate dueDate) {
        Assignment a = new Assignment();
        a.setId(UUID.randomUUID());
        a.setDueDate(dueDate);
        return a;
    }

    private Availability uniformAvailability(int hoursPerDay) {
        Availability av = new Availability();
        av.setMonHours(hoursPerDay);
        av.setTueHours(hoursPerDay);
        av.setWedHours(hoursPerDay);
        av.setThuHours(hoursPerDay);
        av.setFriHours(hoursPerDay);
        av.setSatHours(hoursPerDay);
        av.setSunHours(hoursPerDay);
        return av;
    }
}
