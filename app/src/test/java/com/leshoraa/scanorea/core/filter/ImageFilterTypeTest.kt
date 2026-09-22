package com.leshoraa.scanorea.core.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating [ImageFilterType] metadata.
 */
class ImageFilterTypeTest {

    @Test
    fun imageFilterType_entriesHaveNonEmptyLabels() {
        ImageFilterType.entries.forEach { filter ->
            assertTrue(filter.label.isNotBlank())
            assertTrue(filter.shortName.isNotBlank())
        }
    }

    @Test
    fun originalFilter_hasExpectedProperties() {
        assertEquals("Original Color", ImageFilterType.ORIGINAL.label)
        assertEquals("Color", ImageFilterType.ORIGINAL.shortName)
    }

    @Test
    fun blackAndWhiteFilter_hasExpectedProperties() {
        assertEquals("B&W Document", ImageFilterType.BLACK_AND_WHITE.label)
        assertEquals("B&W", ImageFilterType.BLACK_AND_WHITE.shortName)
    }
}
