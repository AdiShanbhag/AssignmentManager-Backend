package com.applicationplanner.api.util;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

public class WithMockUserId {

    public static final UUID TEST_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    public static void set() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                TEST_USER_ID, null, List.of()  // UUID directly, not toString()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}