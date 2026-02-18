package com.applicationplanner.api.dto.requestDTO;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateAssignmentRequest (
    @NotBlank String title,
    @NotBlank String subject,
    @NotNull @FutureOrPresent LocalDate dueDate
){}
