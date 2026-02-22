package com.applicationplanner.api.model;

import com.applicationplanner.api.enums.UnscheduledReason;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "is_done", nullable = false)
    private boolean isDone;

    @Column(name = "target_date")
    private LocalDate targetDate; // nullable = unscheduled equivalent

    @Column(name = "effort_hours", nullable = false)
    private int effortHours;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "is_unscheduled", nullable = false)
    private boolean isUnscheduled;

    @Transient
    private UnscheduledReason unscheduledReason;

    public Task() {}
}