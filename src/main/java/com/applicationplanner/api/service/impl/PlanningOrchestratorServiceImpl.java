package com.applicationplanner.api.service.impl;

import com.applicationplanner.api.enums.PanicStatus;
import com.applicationplanner.api.model.Assignment;
import com.applicationplanner.api.model.Availability;
import com.applicationplanner.api.model.Task;
import com.applicationplanner.api.record.AssignmentPlanView;
import com.applicationplanner.api.repository.AssignmentRepository;
import com.applicationplanner.api.repository.AvailabilityRepository;
import com.applicationplanner.api.repository.TaskRepository;
import com.applicationplanner.api.security.CurrentUser;
import com.applicationplanner.api.service.PlannerService;
import com.applicationplanner.api.service.PlanningOrchestratorService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        UUID userId = CurrentUser.requireUserId();

        assignmentInput.setUserId(userId);

        Instant createdAt = Instant.now();
        assignmentInput.setCreatedAt(createdAt);

        int planningDays = computePlanningDaysAtCreation(today, assignmentInput.getDueDate());
        assignmentInput.setPlanningDays(planningDays);

        Assignment saved = assignmentRepository.save(assignmentInput);

        List<Task> tasks = plannerService.generateDefaultTasks(
                saved.getTitle(),
                saved.getDueDate(),
                saved.getId(),
                today
        );
        taskRepository.saveAll(tasks);

        recomputePlan(today);
        return saved;
    }

    @Override
    @Transactional
    public void removeAssignment(UUID assignmentId, LocalDate today) {
        UUID userId = CurrentUser.requireUserId();
        Assignment owned = requireOwnedAssignment(assignmentId, userId);

        // delete tasks by assignmentId (they’re implicitly owned)
        taskRepository.deleteAll(taskRepository.findByAssignmentId(owned.getId()));
        assignmentRepository.deleteByIdAndUserId(owned.getId(), userId);

        recomputePlan(today);
    }

    @Override
    @Transactional
    public void updateAssignmentAndPlan(UUID assignmentId, Assignment patch, LocalDate today) {
        UUID userId = CurrentUser.requireUserId();
        Assignment existing = requireOwnedAssignment(assignmentId, userId);

        if (patch.getTitle() != null) existing.setTitle(patch.getTitle());
        if (patch.getSubject() != null) existing.setSubject(patch.getSubject());
        if (patch.getDueDate() != null) existing.setDueDate(patch.getDueDate());

        assignmentRepository.save(existing);

        recomputePlan(today);
    }

    @Override
    @Transactional(readOnly = true)
    public Availability getAvailabilityOrDefault() {
        UUID userId = CurrentUser.requireUserId();

        return availabilityRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Availability a = defaultAvailability();
                    a.setUserId(userId);
                    return availabilityRepository.save(a);
                });
    }

    @Override
    @Transactional
    public void setAvailabilityAndPlan(Availability nextAvailability, LocalDate today) {
        UUID userId = CurrentUser.requireUserId();

        Availability existing = availabilityRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Availability a = defaultAvailability();
                    a.setUserId(userId);
                    return a;
                });

        existing.setMonHours(nextAvailability.getMonHours());
        existing.setTueHours(nextAvailability.getTueHours());
        existing.setWedHours(nextAvailability.getWedHours());
        existing.setThuHours(nextAvailability.getThuHours());
        existing.setFriHours(nextAvailability.getFriHours());
        existing.setSatHours(nextAvailability.getSatHours());
        existing.setSunHours(nextAvailability.getSunHours());

        availabilityRepository.save(existing);

        recomputePlan(today);
    }

    @Override
    @Transactional
    public void updateTaskEffortAndPlan(UUID assignmentId, UUID taskId, int effortHours, LocalDate today) {
        UUID userId = CurrentUser.requireUserId();
        requireOwnedAssignment(assignmentId, userId);

        Task t = requireTaskInAssignment(taskId, assignmentId);

        // enforce frontend clamp: 0..24 (logic match)
        int hours = Math.max(0, Math.min(24, effortHours));
        t.setEffortHours(hours);

        taskRepository.save(t);

        recomputePlan(today);
    }

    @Override
    @Transactional
    public void updateTaskTitleAndPlan(UUID assignmentId, UUID taskId, String title, LocalDate today) {
        UUID userId = CurrentUser.requireUserId();
        requireOwnedAssignment(assignmentId, userId);

        Task t = requireTaskInAssignment(taskId, assignmentId);

        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) return; // matches frontend (no update if empty)

        t.setTitle(trimmed);
        taskRepository.save(t);

        recomputePlan(today);
    }

    @Override
    @Transactional
    public void toggleTaskDoneAndPlan(UUID assignmentId, UUID taskId, LocalDate today) {
        UUID userId = CurrentUser.requireUserId();
        requireOwnedAssignment(assignmentId, userId);

        Task t = requireTaskInAssignment(taskId, assignmentId);

        t.setDone(!t.isDone());
        taskRepository.save(t);

        List<Task> tasks = taskRepository.findByAssignmentId(assignmentId);
        List<Task> shifted = plannerService.applyMissedTaskShift(tasks, today);
        taskRepository.saveAll(shifted);

        recomputePlan(today);
    }

    @Override
    @Transactional
    public void recomputePlan(LocalDate today) {
        UUID userId = CurrentUser.requireUserId();

        List<Assignment> assignments = assignmentRepository.findAllByUserIdOrderByDueDateAsc(userId);

        assignments.sort(Comparator
                .comparing(Assignment::getDueDate)
                .thenComparing(a -> Optional.ofNullable(a.getCreatedAt()).orElse(Instant.EPOCH))
                .thenComparing(Assignment::getId)
        );

        Availability availability = getAvailabilityOrDefault();

        List<UUID> ids = assignments.stream().map(Assignment::getId).toList();
        List<Task> allTasks = ids.isEmpty() ? List.of() : taskRepository.findAllByAssignmentIdIn(ids);

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
        UUID userId = CurrentUser.requireUserId();
        List<Assignment> assignments = assignmentRepository.findAllByUserIdOrderByDueDateAsc(userId);

        // Sort the same way global planning does: earliest due date first (tie-breaker: createdAt/id)
        assignments.sort(Comparator
                .comparing(Assignment::getDueDate)
                .thenComparing(a -> Optional.ofNullable(a.getCreatedAt()).orElse(Instant.EPOCH))
                .thenComparing(Assignment::getId)
        );

        Availability availability = getAvailabilityOrDefault();

        List<UUID> ids = assignments.stream().map(Assignment::getId).toList();
        List<Task> allTasks = ids.isEmpty() ? List.of() : taskRepository.findAllByAssignmentIdIn(ids);

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

    private Assignment requireOwnedAssignment(UUID assignmentId, UUID userId) {
        return assignmentRepository.findByIdAndUserId(assignmentId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Map<UUID, List<Task>> groupTasksByAssignmentId(List<Task> tasks) {
        Map<UUID, List<Task>> map = new HashMap<>();
        for (Task t : tasks) {
            map.computeIfAbsent(t.getAssignmentId(), k -> new ArrayList<>()).add(t);
        }
        return map;
    }

    private Task requireTaskInAssignment(UUID taskId, UUID assignmentId) {
        Task t = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!t.getAssignmentId().equals(assignmentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return t;
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