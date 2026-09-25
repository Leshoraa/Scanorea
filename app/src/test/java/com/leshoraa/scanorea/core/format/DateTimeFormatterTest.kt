package com.leshoraa.scanorea.core.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class DateTimeFormatterTest {

    @Test
    fun testJustNow() {
        val now = 1000000000L
        assertEquals("Just now", DateTimeFormatter.formatRelativeTime(now - 10000, now))
    }

    @Test
    fun testMinutesAgo() {
        val now = 1000000000L
        assertEquals("2 minutes ago", DateTimeFormatter.formatRelativeTime(now - TimeUnit.MINUTES.toMillis(2), now))
        assertEquals("1 minute ago", DateTimeFormatter.formatRelativeTime(now - TimeUnit.MINUTES.toMillis(1), now))
    }

    @Test
    fun testHoursAgo() {
        val now = 1000000000L
        assertEquals("1 hour ago", DateTimeFormatter.formatRelativeTime(now - TimeUnit.HOURS.toMillis(1), now))
        assertEquals("3 hours ago", DateTimeFormatter.formatRelativeTime(now - TimeUnit.HOURS.toMillis(3), now))
    }

    @Test
    fun testDaysAgo() {
        val now = 1000000000L
        assertEquals("1 day ago", DateTimeFormatter.formatRelativeTime(now - TimeUnit.DAYS.toMillis(1), now))
        assertEquals("3 days ago", DateTimeFormatter.formatRelativeTime(now - TimeUnit.DAYS.toMillis(3), now))
    }
}
