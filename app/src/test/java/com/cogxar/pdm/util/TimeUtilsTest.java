package com.cogxar.pdm.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import org.junit.Test;

/**
 * Unit tests for TimeUtils ISO-8601 parsing, local formatting, relative time, and staleness detection.
 */
public class TimeUtilsTest {

    @Test
    public void testParseIsoToEpochMillis_validUtcTimestamp() {
        String iso = "2026-10-14T04:51:00Z";
        long millis = TimeUtils.parseIsoToEpochMillis(iso);
        assertTrue("Epoch millis must be positive", millis > 0);
    }

    @Test
    public void testParseIsoToEpochMillis_invalidTimestamp_returnsMinusOne() {
        assertEquals(-1, TimeUtils.parseIsoToEpochMillis("not-a-timestamp"));
        assertEquals(-1, TimeUtils.parseIsoToEpochMillis(null));
        assertEquals(-1, TimeUtils.parseIsoToEpochMillis(""));
    }

    @Test
    public void testFormatToLocalTime_deterministicUtc() {
        String iso = "2026-10-14T10:21:00Z";
        String formatted = TimeUtils.formatToLocalTime(iso, ZoneId.of("UTC"));
        assertEquals("Oct 14, 2026, 10:21 AM", formatted);
    }

    @Test
    public void testFormatRelativeTime_calculations() {
        long now = 1000000000000L;

        // Exactly now / under a minute
        String isoJustNow = java.time.Instant.ofEpochMilli(now - 15000).toString();
        assertEquals("just now", TimeUtils.formatRelativeTime(isoJustNow, now));

        // 1 minute ago
        String isoOneMin = java.time.Instant.ofEpochMilli(now - 65000).toString();
        assertEquals("1 min ago", TimeUtils.formatRelativeTime(isoOneMin, now));

        // 15 minutes ago
        String iso15Min = java.time.Instant.ofEpochMilli(now - (15 * 60 * 1000)).toString();
        assertEquals("15 mins ago", TimeUtils.formatRelativeTime(iso15Min, now));

        // 1 hour ago
        String isoOneHr = java.time.Instant.ofEpochMilli(now - (65 * 60 * 1000)).toString();
        assertEquals("1 hr ago", TimeUtils.formatRelativeTime(isoOneHr, now));

        // 4 hours ago
        String iso4Hr = java.time.Instant.ofEpochMilli(now - (4 * 3600 * 1000)).toString();
        assertEquals("4 hrs ago", TimeUtils.formatRelativeTime(iso4Hr, now));

        // 2 days ago
        String iso2Days = java.time.Instant.ofEpochMilli(now - (48 * 3600 * 1000)).toString();
        assertEquals("2 days ago", TimeUtils.formatRelativeTime(iso2Days, now));
    }

    @Test
    public void testIsStale_freshReadingReturnsFalse() {
        long now = System.currentTimeMillis();
        // Reading is 10 seconds old; threshold is 30 seconds
        String freshTs = java.time.Instant.ofEpochMilli(now - 10000).toString();
        assertFalse(TimeUtils.isStale(freshTs, 30L, now));
    }

    @Test
    public void testIsStale_readingOlderThanThresholdReturnsTrue() {
        long now = System.currentTimeMillis();
        // Reading is 45 seconds old; threshold is 30 seconds
        String staleTs = java.time.Instant.ofEpochMilli(now - 45000).toString();
        assertTrue(TimeUtils.isStale(staleTs, 30L, now));
    }

    @Test
    public void testIsStale_nullOrInvalidTimestampReturnsTrue() {
        long now = System.currentTimeMillis();
        assertTrue(TimeUtils.isStale(null, 30L, now));
        assertTrue(TimeUtils.isStale("invalid-date", 30L, now));
    }
}
