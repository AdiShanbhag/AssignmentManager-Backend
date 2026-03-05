package com.applicationplanner.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email")
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "university")
    private String university;

    @Builder.Default
    @Column(name = "timezone", nullable = false)
    private String timezone = "UTC";

    // By Claude - Notification preference fields for Story 11.1
    @Builder.Default
    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled = true;

    @Builder.Default
    @Column(name = "daily_reminder_enabled", nullable = false)
    private boolean dailyReminderEnabled = true;

    @Builder.Default
    @Column(name = "daily_reminder_time", nullable = false)
    private String dailyReminderTime = "08:00";

    @Builder.Default
    @Column(name = "due_date_warning_enabled", nullable = false)
    private boolean dueDateWarningEnabled = true;

    @Builder.Default
    @Column(name = "due_date_warning_days_before", nullable = false)
    private int dueDateWarningDaysBefore = 2;

    @Builder.Default
    @Column(name = "at_risk_alert_enabled", nullable = false)
    private boolean atRiskAlertEnabled = true;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        var now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}