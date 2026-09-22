package com.leshoraa.scanorea.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests validating [FileSizeFormatter].
 */
class FileSizeFormatterTest {

    @Test
    fun format_underZeroOrZero_returnsZeroKb() {
        assertEquals("0 KB", FileSizeFormatter.format(0L))
        assertEquals("0 KB", FileSizeFormatter.format(-10L))
    }

    @Test
    fun format_kilobytes_returnsFormattedKb() {
        assertEquals("100 KB", FileSizeFormatter.format(102400L))
    }

    @Test
    fun format_megabytes_returnsFormattedMb() {
        assertEquals("2.5 MB", FileSizeFormatter.format((2.5 * 1024 * 1024).toLong()))
    }
}
