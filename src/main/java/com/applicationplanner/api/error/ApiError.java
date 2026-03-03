package com.applicationplanner.api.error;

import java.time.Instant;
import java.util.List;

public record ApiError(
        String timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldViolation> violations
) {
    public record FieldViolation(String field, String message) {}

    public static ApiError of(int status, String code, String message, String path, List<FieldViolation> violations) {
        return new ApiError(
                Instant.now().toString(),
                status,
                code,
                message,
                path,
                violations == null ? List.of() : violations
        );
    }

    public static ApiError simple(int status, String code, String message, String path) {
        return of(status, code, message, path, List.of());
    }
}