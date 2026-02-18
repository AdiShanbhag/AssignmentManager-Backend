package com.applicationplanner.api.service.impl;

import com.applicationplanner.api.enums.PanicStatus;
import com.applicationplanner.api.model.Assignment;
import com.applicationplanner.api.model.Availability;
import com.applicationplanner.api.model.Task;
import com.applicationplanner.api.record.AssignmentPlanView;
import com.applicationplanner.api.repository.AssignmentRepository;
import com.applicationplanner.api.repository.AvailabilityRepository;
import com.applicationplanner.api.repository.TaskRepository;
import com.applicationplanner.api.service.PlannerService;
import com.applicationplanner.api.service.PlanningOrchestratorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static com.applicationplanner.api.model.Availability.getAvailability;

@Service
@Transactional
public class PlanningOrchestratorServiceImpl implements PlanningOrchestratorService {

    private final AssignmentRepository assignmentRepository;
    private final TaskRepository taskRepository;
    private final AvailabilityRepository availabilityRepository;
    private final PlannerService plannerService;

    public PlanningOrchestratorServiceImpl(
            AssignmentRepository assignmentRepository,
            TaskRepository taskRepository,
            AvailabilityRepository availabilityRepository,
            PlannerService plannerService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.taskRepository = taskRepository;
        this.availabilityRepository = availabilityRepository;
        this.plannerService = plannerService;
    }

    @Override
    @Transactional
    public Assignment createAssignmentAndPlan(Assignment assignmentInput, LocalDate today) {
        // planningDays fixed at creation: creation day -> due-1 (inclusive)
        Instant createdAt = Instant.now();
        assignmentInput.setCreatedAt(createdAt);

        // createdDay should match frontend "startOfDay(new Date(createdAt))"
        // easiest: use today passed into method (same day semantics)
        int planningDays = computePlanningDaysAtCreation(today, assignmentInput.getDueDate());
        assignmentInput.setPlanningDays(planningDays);

        Assignment saved = assignmentRepository.save(assignmentInput);

        // Generate default tasks (exactly as frontend does now)
        List<Task> tasks = plannerService.generateDefaultTasks(
                saved.getTitle(),
                saved.getDueDate(),
                saved.getId(),
                today
        );
        taskRepository.saveAll(tasks);

        // Then global plan (frontend effect)
        recomputePlan(today);

        return saved;
    }

    @Override
    @Transactional
    public void removeAssignment(UUID assignmentId, LocalDate today) {
        // delete tasks first (unless you have cascade)
        List<Task> tasks = taskRepository.findByAssignmentId(assignmentId);
        taskRepository.deleteAll(tasks);
        assignmentRepository.deleteById(assignmentId);

        recomputePlan(today);
    }

    @Override
    @Transactional
    public void updateAssignmentAndPlan(UUID assignmentId, Assignment patch, LocalDate today) {
        Assignment existing = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        if (patch.getTitle() != null) existing.setTitle(patch.getTitle());
        if (patch.getSubject() != null) existing.setSubject(patch.getSubject());
        if (patch.getDueDate() != null) existing.setDueDate(patch.getDueDate());

        // planningDays stays fixed (matches frontend)
        assignmentRepository.save(existing);

        // frontend triggers global plan via assignments change
        recomputePlan(today);
    }

    @Override
    @Transactional(readOnly = true)
    public Availability getAvailabilityOrDefault() {
        return availabilityRepository.findAll()
                .stream()
                .findFirst()
                .orElse(defaultAvailability());
    }

    @Override
    @Transactional
    public void setAvailabilityAndPlan(Availability nextAvailability, LocalDate today) {
        // single-row approach: replace existing or create new
        List<Availability> all = availabilityRepository.findAll();
        if (!all.isEmpty()) {
            Availability existing = all.get(0);
            existing.setMonHours(nextAvailability.getMonHours());
            existing.setTueHours(nextAvailability.getTueHours());
            existing.setWedHours(nextAvailability.getWedHours());
            existing.setThuHours(nextAvailability.getThuHours());
            existing.setFriHours(nextAvailability.getFriHours());
            existing.setSatHours(nextAvailability.getSatHours());
            existing.setSunHours(nextAvailability.getSunHours());
            availabilityRepository.save(existing);
        } else {
            availabilityRepository.save(nextAvailability);
        }

        recomputePlan(today);
    }

    @Override
    @Transactional
    public void updateTaskEffortAndPlan(UUID assignmentId, UUID taskId, int effortHours, LocalDate today) {
        Task t = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // enforce frontend clamp: 0..24 (logic match)
        int hours = Math.max(0, Math.min(24, effortHours));
        t.setEffortHours(hours);

        taskRepository.save(t);

        recomputePlan(today);
    }

    @Override
    @Transactional
    public void updateTaskTitleAndPlan(UUID assignmentId, UUID taskId, String title, LocalDate today) {
        Task t = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) return; // matches frontend (no update if empty)

        t.setTitle(trimmed);
        taskRepository.save(t);

        recomputePlan(today);
    }

    @Override
    @Transactional
    public void toggleTaskDoneAndPlan(UUID assignmentId, UUID taskId, LocalDate today) {
        Task t = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        t.setDone(!t.isDone());
        taskRepository.save(t);

        // applyMissedTaskShift to ALL tasks of this assignment (matches frontend toggleTask)
        List<Task> tasks = taskRepository.findByAssignmentId(assignmentId);
        List<Task> shifted = plannerService.applyMissedTaskShift(tasks, today);
        taskRepository.saveAll(shifted);

        // then global plan (frontend effect runs after tasksById changes)
        recomputePlan(today);
    }

    @Override
    @Transactional
    public void recomputePlan(LocalDate today) {
        List<Assignment> assignments = assignmentRepository.findAll();

        assignments.sort(Comparator
                .comparing(Assignment::getDueDate)
                .thenComparing(a -> Optional.ofNullable(a.getCreatedAt()).orElse(Instant.EPOCH))
                .thenComparing(Assignment::getId)
        );

        List<Task> allTasks = taskRepository.findAll();
        Availability availability = getAvailabilityOrDefault();

        Map<UUID, List<Task>> tasksByAssignmentId = groupTasksByAssignmentId(allTasks);

        Map<UUID, List<Task>> planned = plannerService.buildGlobalPlan(
                assignments,
                tasksByAssignmentId,
                availability,
                today
        );

        List<Task> flattened = new ArrayList<>();
        for (List<Task> list : planned.values()) flattened.addAll(list);

        taskRepository.saveAll(flattened);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentPlanView> getPlanView(LocalDate today) {
        List<Assignment> assignments = assignmentRepository.findAll();

        // Sort the same way global planning does: earliest due date first (tie-breaker: createdAt/id)
        assignments.sort(Comparator
                .comparing(Assignment::getDueDate)
                .thenComparing(a -> Optional.ofNullable(a.getCreatedAt()).orElse(Instant.EPOCH))
                .thenComparing(Assignment::getId)
        );

        Availability availability = getAvailabilityOrDefault();

        List<Task> allTasks = taskRepository.findAll();
        Map<UUID, List<Task>> grouped = groupTasksByAssignmentId(allTasks);

        Map<UUID, List<Task>> planned = plannerService.buildGlobalPlan(
                assignments,
                grouped,
                availability,
                today
        );

        List<AssignmentPlanView> result = new ArrayList<>();
        for (Assignment a : assignments) {
            List<Task> tasks = planned.getOrDefault(a.getId(), List.of());
            PanicStatus status = plannerService.computePanicStatusV2(a, tasks, today);

            int consumed = hoursConsumedByEarlierAssignments(assignments, planned, a, today);

            result.add(new AssignmentPlanView(a, status, tasks, consumed));
        }
        return result;
    }

    // -----------------------
    // Helpers
    // -----------------------

    private Map<UUID, List<Task>> groupTasksByAssignmentId(List<Task> tasks) {
        Map<UUID, List<Task>> map = new HashMap<>();
        for (Task t : tasks) {
            map.computeIfAbsent(t.getAssignmentId(), k -> new ArrayList<>()).add(t);
        }
        return map;
    }

    private Availability defaultAvailability() {
        return getAvailability();
    }

    private int computePlanningDaysAtCreation(LocalDate createdDay, LocalDate dueDate) {
        LocalDate end = dueDate.minusDays(1);
        long days = java.time.temporal.ChronoUnit.DAYS.between(createdDay, end);
        return Math.max(1, (int) days + 1);
    }

    private int hoursConsumedByEarlierAssignments(
            List<Assignment> sortedAssignments,
            Map<UUID, List<Task>> plannedTasksByAssignmentId,
            Assignment current,
            LocalDate today
    ) {
        LocalDate end = current.getDueDate().minusDays(1);

        int total = 0;
        for (Assignment a : sortedAssignments) {
            if (a.getId().equals(current.getId())) break;

            List<Task> tasks = plannedTasksByAssignmentId.getOrDefault(a.getId(), List.of());
            for (Task t : tasks) {
                if (t.isDone()) continue;
                LocalDate d = t.getTargetDate();
                if (d == null) continue;

                boolean withinWindow =
                        (d.isEqual(today) || d.isAfter(today)) &&
                                (d.isEqual(end) || d.isBefore(end));

                if (withinWindow) total += t.getEffortHours();
            }
        }
        return total;
    }
}