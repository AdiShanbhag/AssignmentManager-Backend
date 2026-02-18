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

    @Column(nullable = false)
    private UUID assignmentId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private boolean isDone;

    private LocalDate targetDate; // nullable = unscheduled equivalent of ""

    @Column(nullable = false)
    private int effortHours;

    @Column(nullable = false)
    private int orderIndex;

    @Column(nullable = false)
    private boolean isUnscheduled;

    @Transient
    private UnscheduledReason unscheduledReason;

    public Task(){}
}
