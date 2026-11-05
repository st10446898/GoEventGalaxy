package com.goevent.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattersTest {
    @Test fun rand_dropsZeroCents() {
        assertEquals("R165", Formatters.rand(16_500))
        assertEquals("R165.50", Formatters.rand(16_550))
        assertEquals("R0", Formatters.rand(0))
    }

    @Test fun date_formatsIso() {
        assertEquals("Wed, 4 Nov 2026", Formatters.date("2026-11-04"))
        assertEquals("4 Nov", Formatters.shortDate("2026-11-04"))
        assertEquals("4" to "NOV", Formatters.dayAndMonth("2026-11-04"))
    }

    @Test fun time_trimsSeconds() = assertEquals("18:00", Formatters.time("18:00:00"))

    @Test fun initials_andFirstName() {
        assertEquals("NK", Formatters.initials("Ntombi Kayise"))
        assertEquals("N", Formatters.initials("Ntombi"))
        assertEquals("Ntombi", Formatters.firstName("Ntombi Kayise"))
        assertEquals("Explorer", Formatters.firstName("  "))
    }

    @Test fun greeting_followsHour() {
        assertEquals("Good morning", Formatters.greeting(8))
        assertEquals("Good afternoon", Formatters.greeting(14))
        assertEquals("Good evening", Formatters.greeting(20))
    }

    @Test fun relativeTime_usesElapsedMinutes() {
        val now = 1_700_000_000_000L // fixed clock
        val fiveMinAgo = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.ENGLISH)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date(now - 5 * 60_000))
        assertEquals("5 min ago", Formatters.relativeTime("$fiveMinAgo+00:00", now))
        assertEquals("", Formatters.relativeTime(null, now))
    }

    @Test fun isUpcoming_comparesWithToday() {
        assertTrue(Formatters.isUpcoming("2999-01-01"))
        assertFalse(Formatters.isUpcoming("2000-01-01"))
    }

    @Test fun dateRange_anyHasNoLimits() {
        assertEquals(null to null, Formatters.dateRange("Any"))
        val (from, to) = Formatters.dateRange("Today")
        assertEquals(from, to)
    }
}
