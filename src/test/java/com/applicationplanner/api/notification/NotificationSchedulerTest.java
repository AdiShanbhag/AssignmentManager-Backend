package com.applicationplanner.api.notification;

import com.applicationplanner.api.enums.PanicStatus;
import com.applicationplanner.api.model.Assignment;
import com.applicationplanner.api.model.Availability;
import com.applicationplanner.api.model.DeviceToken;
import com.applicationplanner.api.model.Task;
import com.applicationplanner.api.model.User;
import com.applicationplanner.api.repository.AssignmentRepository;
import com.applicationplanner.api.repository.AvailabilityRepository;
import com.applicationplanner.api.repository.DeviceTokenRepository;
import com.applicationplanner.api.repository.TaskRepository;
import com.applicationplanner.api.repository.UserRepository;
import com.applicationplanner.api.service.PlannerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock private UserRepository userRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private AvailabilityRepository availabilityRepository;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private PlannerService plannerService;
    @Mock private FcmService fcmService;

    @InjectMocks
    private NotificationScheduler scheduler;

    private User testUser;
    private UUID userId;
    private DeviceToken deviceToken;

    private static final LocalDate TODAY_UTC = LocalDate.now(ZoneId.of("UTC"));

    /**
     * Sets up a default user and device token before each test. By Claude
     */
    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        String currentTime = String.format("%02d:%02d", now.getHour(), now.getMinute());

        testUser = User.builder()
                .id(userId)
                .email("test@test.com")
                .displayName("Test User")
                .timezone("UTC")
                .notificationsEnabled(true)
                .dailyReminderEnabled(true)
                .dailyReminderTime(currentTime)
                .dueDateWarningEnabled(true)
                .dueDateWarningDaysBefore(2)
                .atRiskAlertEnabled(true)
                .build();

        deviceToken = DeviceToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .token("test-fcm-token")
                .platform("android")
                .build();
    }

    // -------------------------
    // Daily Reminder Job Tests
    // -------------------------

    /**
     * Verifies daily reminder is sent when user has incomplete assignments at reminder time. By Claude
     */
    @Test
    void dailyReminderJob_sendsNotification_whenUserHasIncompleteAssignments() {
        Assignment assignment = buildAssignment(userId, TODAY_UTC.plusDays(5));
        Task incompleteTask = buildTask(assignment.getId(), false);

        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(assignmentRepository.findAllByUserIdOrderByDueDateAsc(userId))
                .thenReturn(List.of(assignment));
        when(taskRepository.findByAssignmentId(assignment.getId()))
                .thenReturn(List.of(incompleteTask));
        when(deviceTokenRepository.findAllByUserId(userId))
                .thenReturn(List.of(deviceToken));

        scheduler.runDailyReminderJob();

        verify(fcmService).sendNotification(eq("test-fcm-token"), contains("Daily Study Reminder"), any());
    }

    /**
     * Verifies daily reminder is not sent when notifications are disabled. By Claude
     */
    @Test
    void dailyReminderJob_skipsUser_whenNotificationsDisabled() {
        testUser.setNotificationsEnabled(false);
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        scheduler.runDailyReminderJob();

        verify(fcmService, never()).sendNotification(any(), any(), any());
    }

    /**
     * Verifies daily reminder is not sent when daily reminder is disabled. By Claude
     */
    @Test
    void dailyReminderJob_skipsUser_whenDailyReminderDisabled() {
        testUser.setDailyReminderEnabled(false);
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        scheduler.runDailyReminderJob();

        verify(fcmService, never()).sendNotification(any(), any(), any());
    }

    /**
     * Verifies daily reminder is not sent when user has no assignments. By Claude
     */
    @Test
    void dailyReminderJob_skipsUser_whenNoAssignments() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(assignmentRepository.findAllByUserIdOrderByDueDateAsc(userId))
                .thenReturn(List.of());

        scheduler.runDailyReminderJob();

        verify(fcmService, never()).sendNotification(any(), any(), any());
    }

    /**
     * Verifies daily reminder is not sent when all assignments are complete. By Claude
     */
    @Test
    void dailyReminderJob_skipsUser_whenAllAssignmentsComplete() {
        Assignment assignment = buildAssignment(userId, TODAY_UTC.plusDays(5));
        Task completedTask = buildTask(assignment.getId(), true);

        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(assignmentRepository.findAllByUserIdOrderByDueDateAsc(userId))
                .thenReturn(List.of(assignment));
        when(taskRepository.findByAssignmentId(assignment.getId()))
                .thenReturn(List.of(completedTask));

        scheduler.runDailyReminderJob();

        verify(fcmService, never()).sendNotification(any(), any(), any());
    }

    // -------------------------
    // Due Date Warning Job Tests
    // -------------------------

    /**
     * Verifies due date warning is sent when assignment is due within warning window. By Claude
     */
    @Test
    void dueDateWarningJob_sendsNotification_whenAssignmentDueWithinWarningWindow() {
        Assignment assignment = buildAssignment(userId, TODAY_UTC.plusDays(2));
        Task incompleteTask = buildTask(assignment.getId(), false);

        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(assignmentRepository.findAllByUserIdOrderByDueDateAsc(userId))
                .thenReturn(List.of(assignment));
        when(taskRepository.findByAssignmentId(assignment.getId()))
                .thenReturn(List.of(incompleteTask));
        when(deviceTokenRepository.findAllByUserId(userId))
                .thenReturn(List.of(deviceToken));

        scheduler.runDueDateWarningJob();

        verify(fcmService).sendNotification(eq("test-fcm-token"), contains("Due Soon"), any());
    }

    /**
     * Verifies due date warning is not sent when assignment is not within warning window. By Claude
     */
    @Test
    void dueDateWarningJob_skipsAssignment_whenNotWithinWarningWindow() {
        Assignment assignment = buildAssignment(userId, TODAY_UTC.plusDays(5));

        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(assignmentRepository.findAllByUserIdOrderByDueDateAsc(userId))
                .thenReturn(List.of(assignment));

        scheduler.runDueDateWarningJob();

        verify(fcmService, never()).sendNotification(any(), any(), any());
    }

    /**
     * Verifies due date warning is not sent when all tasks are complete. By Claude
     */
    @Test
    void dueDateWarningJob_skipsAssignment_whenAllTasksComplete() {
        Assignment assignment = buildAssignment(userId, TODAY_UTC.plusDays(2));
        Task completedTask = buildTask(assignment.getId(), true);

        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(assignmentRepository.findAllByUserIdOrderByDueDateAsc(userId))
                .thenReturn(List.of(assignment));
        when(taskRepository.findByAssignmentId(assignment.getId()))
                .thenReturn(List.of(completedTask));

        scheduler.runDueDateWarningJob();

        verify(fcmService, never()).sendNotification(any(), any(), any());
    }

    // -------------------------
    // At Risk Alert Job Tests
    // -------------------------

    /**
     * Verifies at-risk alert is sent when assignment is AT_RISK. By Claude
     */
    @Test
    void atRiskAlertJob_sendsAtRiskNotification_whenAssignmentIsAtRisk() {
        Assignment assignment = buildAssignment(userId, TODAY_UTC.plusDays(2));
        Task incompleteTask = buildTask(assignment.getId(), false);

        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(assignmentRepository.findAllByUserIdOrderByDueDateAsc(userId))
                .thenReturn(List.of(assignment));
        when(taskRepository.findByAssignmentId(assignment.getId()))
                .thenReturn(List.of(incompleteTask));
        when(availabilityRepository.findByUserId(userId))
                .thenReturn(Optional.of(Availability.getAvailability()));
        when(plannerService.computePanicStatusV2(any(), any(), any()))
                .thenReturn(PanicStatus.AT_RISK);
        when(deviceTokenRepository.findAllByUserId(userId))
                .thenReturn(List.of(deviceToken));

        scheduler.runAtRiskAlertJob();

        verify(fcmService).sendNotification(eq("test-fcm-token"), contains("at Risk"), any());
    }

    /**
     * Verifies at-risk alert sends critical notification when assignment is SCREWED. By Claude
     */
    @Test
    void atRiskAlertJob_sendsCriticalNotification_whenAssignmentIsScrewed() {
        Assignment assignment = buildAssignment(userId, TODAY_UTC.plusDays(2));
        Task incompleteTask = buildTask(assignment.getId(), false);

        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(assignmentRepository.findAllByUserIdOrderByDueDateAsc(userId))
                .thenReturn(List.of(assignment));
        when(taskRepository.findByAssignmentId(assignment.getId()))
                .thenReturn(List.of(incompleteTask));
        when(availabilityRepository.findByUserId(userId))
                .thenReturn(Optional.of(Availability.getAvailability()));
        when(plannerService.computePanicStatusV2(any(), any(), any()))
                .thenReturn(PanicStatus.SCREWED);
        when(deviceTokenRepository.findAllByUserId(userId))
                .thenReturn(List.of(deviceToken));

        scheduler.runAtRiskAlertJob();

        verify(fcmService).sendNotification(eq("test-fcm-token"), contains("Critical Risk"), any());
    }

    /**
     * Verifies at-risk alert is not sent when assignment is ON_TRACK. By Claude
     */
    @Test
    void atRiskAlertJob_skipsAssignment_whenOnTrack() {
        Assignment assignment = buildAssignment(userId, TODAY_UTC.plusDays(3));
        Task incompleteTask = buildTask(assignment.getId(), false);

        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(assignmentRepository.findAllByUserIdOrderByDueDateAsc(userId))
                .thenReturn(List.of(assignment));
        when(taskRepository.findByAssignmentId(assignment.getId()))
                .thenReturn(List.of(incompleteTask));
        when(availabilityRepository.findByUserId(userId))
                .thenReturn(Optional.of(Availability.getAvailability()));
        when(plannerService.computePanicStatusV2(any(), any(), any()))
                .thenReturn(PanicStatus.ON_TRACK);

        scheduler.runAtRiskAlertJob();

        verify(fcmService, never()).sendNotification(any(), any(), any());
    }

    /**
     * Verifies at-risk alert is not sent when alert is disabled. By Claude
     */
    @Test
    void atRiskAlertJob_skipsUser_whenAtRiskAlertDisabled() {
        testUser.setAtRiskAlertEnabled(false);
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        scheduler.runAtRiskAlertJob();

        verify(fcmService, never()).sendNotification(any(), any(), any());
    }

    // -------------------------
    // Helpers
    // -------------------------

    /**
     * Builds a test assignment for a given user and due date. By Claude
     */
    private Assignment buildAssignment(UUID userId, LocalDate dueDate) {
        Assignment a = new Assignment();
        a.setId(UUID.randomUUID());
        a.setUserId(userId);
        a.setTitle("Test Assignment");
        a.setSubject("Test");
        a.setDueDate(dueDate);
        a.setPlanningDays(5);
        return a;
    }

    /**
     * Builds a test task with the given completion status. By Claude
     */
    private Task buildTask(UUID assignmentId, boolean isDone) {
        Task t = new Task();
        t.setId(UUID.randomUUID());
        t.setAssignmentId(assignmentId);
        t.setTitle("Test Task");
        t.setDone(isDone);
        t.setEffortHours(1);
        t.setOrderIndex(0);
        t.setUnscheduled(false);
        return t;
    }
}