package com.applicationplanner.api.util;

import java.time.LocalDate;
import java.time.ZoneId;

public final class TimezoneUtil {

    private TimezoneUtil() {}

    /**
     * Resolves today's date from the given IANA timezone string.
     * Falls back to UTC if the timezone is null, blank, or invalid.
     * by Claude
     */
    public static LocalDate resolveToday(String tz) {
        if (tz == null || tz.isBlank()) return LocalDate.now(ZoneId.of("UTC"));
        try {
            return LocalDate.now(ZoneId.of(tz));
        } catch (Exception e) {
            return LocalDate.now(ZoneId.of("UTC"));
        }
    }
}