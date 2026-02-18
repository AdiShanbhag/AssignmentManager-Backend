package com.applicationplanner.api.dto.requestDTO;

import jakarta.validation.constraints.FutureOrPresent;

import java.time.LocalDate;

public record UpdateAssignmentRequest(
        String title,
        String subject,
        @FutureOrPresent LocalDate dueDate
) {}