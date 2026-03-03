package com.applicationplanner.api.util;

import com.applicationplanner.api.repository.UserRepository;
import com.applicationplanner.api.security.CurrentUser;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Component
public class TimezoneResolver {

    private final UserRepository userRepository;

    public TimezoneResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resolves today's date using the current user's stored timezone.
     * Falls back to UTC if the user has no timezone set. by Claude
     */
    public LocalDate resolveToday() {
        UUID userId = CurrentUser.requireUserId();
        return userRepository.findById(userId)
                .map(user -> TimezoneUtil.resolveToday(user.getTimezone()))
                .orElse(LocalDate.now(ZoneId.of("UTC")));
    }
}