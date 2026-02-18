package com.applicationplanner.api.dto.requestDTO;

import jakarta.validation.constraints.NotBlank;

public record UpdateTaskTitleRequest(@NotBlank String title) {}
