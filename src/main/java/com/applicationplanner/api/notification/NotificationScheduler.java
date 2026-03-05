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
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NotificationScheduler {

    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final TaskRepository taskRepository;
    private final AvailabilityRepository availabilityRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PlannerService plannerService;
    private final FcmService fcmService;

    public NotificationScheduler(
            UserRepository userRepository,
            AssignmentRepository assignmentRepository,
            TaskRepository taskRepository,
            AvailabilityRepository availabilityRepository,
            DeviceTokenRepository deviceTokenRepository,
            PlannerService plannerService,
            FcmService fcmService
    ) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.taskRepository = taskRepository;
        this.availabilityRepository = availabilityRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.plannerService = plannerService;
        this.fcmService = fcmService;
    }

    /**
     * Runs every minute and checks which users are at their configured daily reminder time.
     * Sends a daily reminder notification if conditions are met. By Claude
     */
    @Scheduled(cron = "0 * * * * *")
    public void runDailyReminderJob() {
        List<User> users = userRepository.findAll();

        for (User user : users) {
            try {
                if (!user.isNotificationsEnabled() || !user.isDailyReminderEnabled()) continue;

                ZoneId zone = safeZone(user.getTimezone());
                ZonedDateTime nowInUserTz = ZonedDateTime.now(zone);
                LocalTime nowTime = nowInUserTz.toLocalTime();
                LocalTime reminderTime = LocalTime.parse(user.getDailyReminderTime());

                // Fire within the same minute as the configured time
                if (nowTime.getHour() != reminderTime.getHour() ||
                        nowTime.getMinute() != reminderTime.getMinute()) continue;

                List<Assignment> assignments = assignmentRepository
                        .findAllByUserIdOrderByDueDateAsc(user.getId());

                if (assignments.isEmpty()) continue;

                long incompleteCount = countIncompleteAssignments(assignments, user.getId());
                if (incompleteCount == 0) continue;

                String title = "Daily Study Reminder 📚";
                String body = "You have " + incompleteCount + " assignment" +
                        (incompleteCount > 1 ? "s" : "") + " in progress. Stay on track!";

                sendToUser(user.getId(), title, body);

            } catch (Exception e) {
                log.error("Daily reminder failed for user {}", user.getId(), e);
            }
        }
    }

    /**
     * Runs every hour and checks for assignments due within the user's configured warning window.
     * Sends a due date warning notification if conditions are met. By Claude
     */
    //Temporary cron for testing, revert to "0 0 * * * *" after verification
    @Scheduled(cron = "0 0 * * * *")
    public void runDueDateWarningJob() {
        List<User> users = userRepository.findAll();

        for (User user : users) {
            try {
                if (!user.isNotificationsEnabled() || !user.isDueDateWarningEnabled()) continue;

                ZoneId zone = safeZone(user.getTimezone());
                LocalDate todayInUserTz = LocalDate.now(zone);

                List<Assignment> assignments = assignmentRepository
                        .findAllByUserIdOrderByDueDateAsc(user.getId());

                for (Assignment assignment : assignments) {
                    long daysUntilDue = todayInUserTz.until(assignment.getDueDate(),
                            java.time.temporal.ChronoUnit.DAYS);

                    if (daysUntilDue != user.getDueDateWarningDaysBefore()) continue;

                    // Check if assignment still has incomplete tasks
                    List<Task> tasks = taskRepository.findByAssignmentId(assignment.getId());
                    boolean hasIncompleteTasks = tasks.stream().anyMatch(t -> !t.isDone());

                    if (!hasIncompleteTasks) continue;

                    String title = "Assignment Due Soon ⏰";
                    String body = "\"" + assignment.getTitle() + "\" is due in " +
                            daysUntilDue + " day" + (daysUntilDue > 1 ? "s" : "") + "!";

                    sendToUser(user.getId(), title, body);
                }

            } catch (Exception e) {
                log.error("Due date warning failed for user {}", user.getId(), e);
            }
        }
    }

    /**
     * Runs every hour and checks for assignments that are AT_RISK or SCREWED.
     * Sends an at-risk alert notification if conditions are met. By Claude
     */
    @Scheduled(cron = "0 30 * * * *")
    public void runAtRiskAlertJob() {
        List<User> users = userRepository.findAll();

        for (User user : users) {
            try {
                if (!user.isNotificationsEnabled() || !user.isAtRiskAlertEnabled()) continue;

                ZoneId zone = safeZone(user.getTimezone());
                LocalDate todayInUserTz = LocalDate.now(zone);

                List<Assignment> assignments = assignmentRepository
                        .findAllByUserIdOrderByDueDateAsc(user.getId());

                Availability availability = availabilityRepository
                        .findByUserId(user.getId())
                        .orElseGet(() -> Availability.getAvailability());

                for (Assignment assignment : assignments) {
                    List<Task> tasks = taskRepository.findByAssignmentId(assignment.getId());
                    PanicStatus status = plannerService.computePanicStatusV2(
                            assignment, tasks, todayInUserTz
                    );

                    if (status == PanicStatus.ON_TRACK) continue;

                    String title = status == PanicStatus.SCREWED
                            ? "Assignment at Critical Risk 🚨"
                            : "Assignment at Risk ⚠️";

                    String body = status == PanicStatus.SCREWED
                            ? "\"" + assignment.getTitle() + "\" is critically behind schedule!"
                            : "\"" + assignment.getTitle() + "\" is falling behind. Time to catch up!";

                    sendToUser(user.getId(), title, body);
                }

            } catch (Exception e) {
                log.error("At-risk alert failed for user {}", user.getId(), e);
            }
        }
    }

    /**
     * Sends a notification to all registered device tokens for a user. By Claude
     */
    private void sendToUser(UUID userId, String title, String body) {
        List<DeviceToken> tokens = deviceTokenRepository.findAllByUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("No device tokens found for user {}", userId);
            return;
        }
        for (DeviceToken token : tokens) {
            fcmService.sendNotification(token.getToken(), title, body);
        }
    }

    /**
     * Counts assignments with at least one incomplete task for a given user. By Claude
     */
    private long countIncompleteAssignments(List<Assignment> assignments, UUID userId) {
        return assignments.stream().filter(a -> {
            List<Task> tasks = taskRepository.findByAssignmentId(a.getId());
            return tasks.stream().anyMatch(t -> !t.isDone());
        }).count();
    }

    /**
     * Safely parses a timezone string, falling back to UTC if invalid. By Claude
     */
    private ZoneId safeZone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.of("UTC");
        }
    }
}