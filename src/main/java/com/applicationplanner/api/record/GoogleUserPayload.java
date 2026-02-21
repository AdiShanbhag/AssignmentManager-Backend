package com.applicationplanner.api.record;

public record GoogleUserPayload(
        String subject,     // sub
        String email,
        String name
) {}
