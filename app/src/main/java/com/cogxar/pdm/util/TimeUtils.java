package com.cogxar.pdm.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Utility functions for parsing ISO-8601 UTC timestamps, formatting for local display,
 * computing relative times ("1 min ago"), and detecting stale data.
 */
public final class TimeUtils {

    private static final DateTimeFormatter LOCAL_DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.US);

    private static final DateTimeFormatter TIME_ONLY_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private TimeUtils() {
        // Utility class
    }

    /**
     * Formats an ISO-8601 UTC timestamp to short local time (e.g. "10:21 AM").
     */
    public static String formatTimeOnly(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.trim().isEmpty()) {
            return "—";
        }
        try {
            Instant instant = Instant.parse(isoTimestamp.trim());
            ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
            return zdt.format(TIME_ONLY_FORMAT);
        } catch (DateTimeParseException e) {
            return isoTimestamp;
        }
    }

    /**
     * Parses an ISO-8601 UTC string to epoch milliseconds.
     *
     * @param isoTimestamp e.g. "2026-10-14T04:51:00Z"
     * @return epoch milliseconds, or -1 if invalid
     */
    public static long parseIsoToEpochMillis(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.trim().isEmpty()) {
            return -1;
        }
        try {
            return Instant.parse(isoTimestamp.trim()).toEpochMilli();
        } catch (DateTimeParseException e) {
            return -1;
        }
    }

    /**
     * Formats an ISO-8601 UTC timestamp into a human-readable local date/time string.
     * Example: "Oct 14, 2026, 10:21 AM"
     *
     * @param isoTimestamp e.g. "2026-10-14T04:51:00Z"
     * @return Formatted local time string, or "—" if timestamp is invalid/null
     */
    public static String formatToLocalTime(String isoTimestamp) {
        return formatToLocalTime(isoTimestamp, ZoneId.systemDefault());
    }

    /**
     * Overload for deterministic unit testing with an explicit ZoneId.
     */
    public static String formatToLocalTime(String isoTimestamp, ZoneId zoneId) {
        if (isoTimestamp == null || isoTimestamp.trim().isEmpty()) {
            return "—";
        }
        try {
            Instant instant = Instant.parse(isoTimestamp.trim());
            ZonedDateTime zdt = instant.atZone(zoneId);
            return zdt.format(LOCAL_DISPLAY_FORMAT);
        } catch (DateTimeParseException e) {
            return isoTimestamp;
        }
    }

    /**
     * Formats relative time ("just now", "1 min ago", "5 mins ago", etc.) compared to current time.
     *
     * @param isoTimestamp ISO-8601 UTC string
     * @return Relative time string
     */
    public static String formatRelativeTime(String isoTimestamp) {
        return formatRelativeTime(isoTimestamp, System.currentTimeMillis());
    }

    /**
     * Overload for deterministic unit testing with explicit nowMillis.
     */
    public static String formatRelativeTime(String isoTimestamp, long nowMillis) {
        long tsMillis = parseIsoToEpochMillis(isoTimestamp);
        if (tsMillis <= 0) {
            return "unknown";
        }

        long diffMillis = nowMillis - tsMillis;
        if (diffMillis < 0) {
            return "just now";
        }

        long diffSec = diffMillis / 1000L;
        if (diffSec < 60) {
            return "just now";
        }

        long diffMin = diffSec / 60L;
        if (diffMin == 1) {
            return "1 min ago";
        }
        if (diffMin < 60) {
            return diffMin + " mins ago";
        }

        long diffHr = diffMin / 60L;
        if (diffHr == 1) {
            return "1 hr ago";
        }
        if (diffHr < 24) {
            return diffHr + " hrs ago";
        }

        long diffDays = diffHr / 24L;
        if (diffDays == 1) {
            return "1 day ago";
        }
        return diffDays + " days ago";
    }

    /**
     * Checks if a reading is stale based on a threshold in seconds.
     *
     * @param isoTimestamp      ISO-8601 UTC timestamp of last reading
     * @param staleThresholdSec Threshold in seconds (e.g. 30)
     * @param nowMillis         Current epoch milliseconds
     * @return true if timestamp is older than thresholdSec or unparseable; false otherwise
     */
    public static boolean isStale(String isoTimestamp, long staleThresholdSec, long nowMillis) {
        long tsMillis = parseIsoToEpochMillis(isoTimestamp);
        if (tsMillis <= 0) {
            return true;
        }
        long diffSec = (nowMillis - tsMillis) / 1000L;
        return diffSec > staleThresholdSec;
    }
}
