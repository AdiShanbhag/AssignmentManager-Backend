package com.applicationplanner.api.dto.requestDTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateTaskEffortHourRequest(
    @Min(0) @Max(24)
    Integer effortHours // optional
){}
