package com.applicationplanner.api.dto.requestDTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AvailabilityRequest(
        @Min(0) @Max(24) int monHours,
        @Min(0) @Max(24) int tueHours,
        @Min(0) @Max(24) int wedHours,
        @Min(0) @Max(24) int thuHours,
        @Min(0) @Max(24) int friHours,
        @Min(0) @Max(24) int satHours,
        @Min(0) @Max(24) int sunHours
) {}