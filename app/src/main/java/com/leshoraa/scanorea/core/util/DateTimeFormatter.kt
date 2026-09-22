package com.leshoraa.scanorea.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility for formatting timestamps into human-readable relative time strings
 * such as "2 minutes ago", "1 hour ago", "3 hours ago", "1 day ago".
 */
object DateTimeFormatter {

    fun formatRelativeTime(millis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diffMillis = nowMillis - millis
        if (diffMillis < 0) return "Just now"

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diffMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
        val days = TimeUnit.MILLISECONDS.toDays(diffMillis)

        return when {
            seconds < 45 -> "Just now"
            minutes < 60 -> if (minutes <= 1L) "1 minute ago" else "$minutes minutes ago"
            hours < 24 -> if (hours <= 1L) "1 hour ago" else "$hours hours ago"
            days < 7 -> if (days <= 1L) "1 day ago" else "$days days ago"
            days < 30 -> {
                val weeks = (days / 7).coerceAtLeast(1)
                if (weeks <= 1L) "1 week ago" else "$weeks weeks ago"
            }
            else -> {
                val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                formatter.format(Date(millis))
            }
        }
    }
}
