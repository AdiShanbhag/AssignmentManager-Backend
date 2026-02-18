package com.applicationplanner.api.service.impl;

import com.applicationplanner.api.enums.PanicStatus;
import com.applicationplanner.api.enums.UnscheduledReason;
import com.applicationplanner.api.model.Assignment;
import com.applicationplanner.api.model.Availability;
import com.applicationplanner.api.model.Task;
import com.applicationplanner.api.service.PlannerService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
public class PlannerServiceImpl implements PlannerService {

    @Override
    public List<Task> generateDefaultTasks(String assignmentTitle, LocalDate dueDate, UUID assignmentId, LocalDate today) {
        // Port of generateTasks() exactly (the currently active creation behavior)
        List<String> titles = List.of(
                "Plan: break down \"" + assignmentTitle + "\"",
                "Research / gather materials",
                "Draft / build first version",
                "Review + fix issues",
                "Final polish + submit"
        );

        LocalDate workEnd = workEndFromDue(dueDate);

        int totalDays = Math.max(1, (int) diffDays(today, workEnd) + 1);

        List<Task> tasks = new ArrayList<>();
        for (int index = 0; index < titles.size(); index++) {
            // dayOffset = floor((index / titles.length) * totalDays)
            double frac = (double) index / (double) titles.size();
            int dayOffset = (int) Math.floor(frac * totalDays);

            LocalDate target = today.plusDays(dayOffset);

            Task t = new Task();
            t.setAssignmentId(assignmentId);
            t.setTitle(titles.get(index));
            t.setOrderIndex(index);
            t.setDone(false);
            t.setTargetDate(target);
            t.setEffortHours(1);
            t.setUnscheduled(false);

            tasks.add(t);
        }
        return tasks;
    }

    @Override
    public Map<UUID, List<Task>> buildGlobalPlan(
            List<Assignment> assignments,
            Map<UUID, List<Task>> tasksByAssignmentId,
            Availability availability,
            LocalDate today
    ) {
        if (assignments == null || assignments.isEmpty()) return tasksByAssignmentId;

        // maxEnd = max(workEndFromDue(a.dueDate)) across assignments (fallback to today)
        LocalDate maxEnd = today;
        for (Assignment a : assignments) {
            LocalDate end = workEndFromDue(a.getDueDate());
            if (end.isAfter(maxEnd)) maxEnd = end;
        }

        Map<LocalDate, Integer> remainingCapacity = buildCapacityMap(today, maxEnd, availability);

        // sort assignments by dueDate ascending (string compare in TS; LocalDate compare is equivalent)
        List<Assignment> sortedAssignments = new ArrayList<>(assignments);
        sortedAssignments.sort(Comparator.comparing(Assignment::getDueDate));

        Map<UUID, List<Task>> next = new HashMap<>(tasksByAssignmentId);

        for (Assignment a : sortedAssignments) {
            UUID assignmentId = a.getId();
            LocalDate end = workEndFromDue(a.getDueDate());

            LocalDate cursor = today; // cursor = todayDate()

            List<Task> currentTasks = next.getOrDefault(assignmentId, List.of());

            boolean chainBroken = false;

            List<Task> updated = new ArrayList<>(currentTasks.size());
            for (Task task : currentTasks) {

                // NOTE: this mirrors TS mapping behavior:
                // - it iterates ALL tasks in stored order (not just sorted incomplete)
                // - isDone tasks stay done and get isUnscheduled=false
                if (task.isDone()) {
                    Task copy = shallowCopy(task);
                    copy.setUnscheduled(false);
                    copy.setUnscheduledReason(null);
                    updated.add(copy);
                    continue;
                }

                if (chainBroken) {
                    Task copy = shallowCopy(task);
                    copy.setUnscheduled(true);
                    copy.setTargetDate(null); // TS uses ""
                    copy.setUnscheduledReason(UnscheduledReason.CHAIN_BROKEN);
                    updated.add(copy);
                    continue;
                }

                AllocationResult result = allocateEffortLinearly(
                        cursor,
                        end,
                        task.getEffortHours(),
                        remainingCapacity,
                        availability
                );

                if (result.endDate == null) {
                    chainBroken = true;
                    Task copy = shallowCopy(task);
                    copy.setUnscheduled(true);
                    copy.setTargetDate(null);
                    copy.setUnscheduledReason(determineReason(cursor, end, availability));
                    updated.add(copy);
                    continue;
                }

                cursor = result.endDate; // TS: cursor = result.endDate

                Task copy = shallowCopy(task);
                copy.setTargetDate(result.endDate);
                copy.setUnscheduled(false);
                copy.setUnscheduledReason(null);

                updated.add(copy);
            }

            next.put(assignmentId, updated);
        }

        return next;
    }

    @Override
    public List<Task> applyMissedTaskShift(List<Task> tasks, LocalDate today) {
        // Port of applyMissedTaskShift()
        List<Task> incomplete = new ArrayList<>();
        for (Task t : tasks) {
            if (!t.isDone()) incomplete.add(t);
        }
        if (incomplete.isEmpty()) return tasks;

        // earliest incomplete targetDate
        LocalDate earliest = null;
        for (Task t : incomplete) {
            LocalDate td = t.getTargetDate();
            if (td == null) continue; // TS has "" which behaves like a weird date; this keeps intended behavior
            if (earliest == null || td.isBefore(earliest)) earliest = td;
        }

        if (earliest == null) return tasks;

        long missedDays = diffDays(earliest, today); // today - earliest
        if (missedDays <= 0) return tasks;

        List<Task> shifted = new ArrayList<>(tasks.size());
        for (Task t : tasks) {
            if (t.isDone()) {
                shifted.add(t);
                continue;
            }
            Task copy = shallowCopy(t);
            if (copy.getTargetDate() != null) {
                copy.setTargetDate(copy.getTargetDate().plusDays(missedDays));
            }
            shifted.add(copy);
        }
        return shifted;
    }

    @Override
    public PanicStatus computePanicStatusV2(Assignment assignment, List<Task> tasks, LocalDate today) {
        LocalDate end = workEndFromDue(assignment.getDueDate());

        // Past work end => already missed the "finish 1 day early" constraint
        if (today.isAfter(end)) return PanicStatus.SCREWED;

        int remainingDays = remainingPlanningDays(assignment.getDueDate(), today); // today..due-1 inclusive

        List<Task> incomplete = new ArrayList<>();
        for (Task t : tasks) if (!t.isDone()) incomplete.add(t);

        if (incomplete.isEmpty()) return PanicStatus.ON_TRACK;

        // 1) Hard infeasible checks based on actual plan output
        boolean hasUnscheduled = false;
        LocalDate latestScheduled = null;

        for (Task t : incomplete) {
            if (t.isUnscheduled() || t.getTargetDate() == null) {
                hasUnscheduled = true;
                break;
            }
            LocalDate td = t.getTargetDate();
            if (td.isAfter(end)) return PanicStatus.SCREWED; // scheduled beyond work window = impossible
            if (latestScheduled == null || td.isAfter(latestScheduled)) latestScheduled = td;
        }

        if (hasUnscheduled) {
            // Keep your old "tight window makes it worse" behavior if you want
            return remainingDays <= 3 ? PanicStatus.SCREWED : PanicStatus.AT_RISK;
        }

        // At this point, the schedule is feasible.
        // 2) Risk based on buffer: ending on end-day is tight, ending earlier is safer.
        if (latestScheduled != null && latestScheduled.equals(end)) return PanicStatus.AT_RISK;

        // 3) Optional: keep heuristics as secondary signals, but never override feasibility
        // Example: if you still want to show AT_RISK when pace is extreme even though feasible
        int remainingTasks = incomplete.size();
        double pace = remainingDays > 0 ? ((double) remainingTasks / (double) remainingDays) : Double.POSITIVE_INFINITY;

        if (remainingDays <= 4 && pace >= 2.0) return PanicStatus.AT_RISK;

        return PanicStatus.ON_TRACK;
    }

    // -----------------------
    // Helpers (TS equivalents)
    // -----------------------

    private LocalDate workEndFromDue(LocalDate dueDate) {
        return dueDate.minusDays(1);
    }

    private long diffDays(LocalDate a, LocalDate b) {
        // b - a, whole days
        return ChronoUnit.DAYS.between(a, b);
    }

    private int remainingPlanningDays(LocalDate dueDate, LocalDate today) {
        LocalDate end = workEndFromDue(dueDate);
        long days = diffDays(today, end);
        return days >= 0 ? (int) days + 1 : 0;
    }

    private Map<LocalDate, Integer> buildCapacityMap(LocalDate start, LocalDate end, Availability availability) {
        Map<LocalDate, Integer> cap = new HashMap<>();
        long total = Math.max(0, diffDays(start, end));
        for (int i = 0; i <= (int) total; i++) {
            LocalDate d = start.plusDays(i);
            int hours = availabilityHoursFor(availability, d.getDayOfWeek());
            cap.put(d, hours);
        }
        return cap;
    }

    private int availabilityHoursFor(Availability a, DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> a.getMonHours();
            case TUESDAY -> a.getTueHours();
            case WEDNESDAY -> a.getWedHours();
            case THURSDAY -> a.getThuHours();
            case FRIDAY -> a.getFriHours();
            case SATURDAY -> a.getSatHours();
            case SUNDAY -> a.getSunHours();
        };
    }

    private AllocationResult allocateEffortLinearly(
            LocalDate startDate,
            LocalDate endDate,
            int effort,
            Map<LocalDate, Integer> remainingCapacity,
            Availability availability
    ) {
        int remaining = effort;
        LocalDate cursor = startDate;

        while (remaining > 0 && (cursor.isBefore(endDate) || cursor.isEqual(endDate))) {
            int cap = remainingCapacity.getOrDefault(cursor, 0);
            int dayAvail = availabilityHoursFor(availability, cursor.getDayOfWeek());

            if (cap > 0 && dayAvail > 0) {
                int used = Math.min(cap, remaining);
                remainingCapacity.put(cursor, cap - used);
                remaining -= used;
            }

            cursor = cursor.plusDays(1);
        }

        if (remaining > 0) return new AllocationResult(null);

        // TS returns endDate = cursor - 1
        return new AllocationResult(cursor.minusDays(1));
    }

    private Task shallowCopy(Task t) {
        Task copy = new Task();
        copy.setId(t.getId());
        copy.setAssignmentId(t.getAssignmentId());
        copy.setTitle(t.getTitle());
        copy.setDone(t.isDone());
        copy.setTargetDate(t.getTargetDate());
        copy.setEffortHours(t.getEffortHours());
        copy.setOrderIndex(t.getOrderIndex());
        copy.setUnscheduled(t.isUnscheduled());
        copy.setUnscheduledReason(t.getUnscheduledReason());
        return copy;
    }

    private static class AllocationResult {
        private final LocalDate endDate;
        private AllocationResult(LocalDate endDate) { this.endDate = endDate; }
    }

    private UnscheduledReason determineReason(LocalDate start, LocalDate end, Availability availability) {
        // If there are ZERO available hours on ANY day in the window, it's basically "window expired / no workable days"
        // (User has no time before due date)
        boolean anyAvailableDay = false;

        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            int hours = hoursFor(cursor, availability);
            if (hours > 0) {
                anyAvailableDay = true;
                break;
            }
            cursor = cursor.plusDays(1);
        }

        return anyAvailableDay ? UnscheduledReason.NO_CAPACITY : UnscheduledReason.WINDOW_EXPIRED;
    }

    private int hoursFor(LocalDate date, Availability availability) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> availability.getMonHours();
            case TUESDAY -> availability.getTueHours();
            case WEDNESDAY -> availability.getWedHours();
            case THURSDAY -> availability.getThuHours();
            case FRIDAY -> availability.getFriHours();
            case SATURDAY -> availability.getSatHours();
            case SUNDAY -> availability.getSunHours();
        };
    }
}