package com.leshoraa.scanorea.core.format

import java.util.Locale

/**
 * Converts byte counts into human-readable formatted strings.
 */
object FileSizeFormatter {

    fun format(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val kb = bytes / 1024f
        val mb = kb / 1024f

        return if (mb >= 1.0f) {
            String.format(Locale.US, "%.1f MB", mb)
        } else {
            String.format(Locale.US, "%.0f KB", kb)
        }
    }
}
