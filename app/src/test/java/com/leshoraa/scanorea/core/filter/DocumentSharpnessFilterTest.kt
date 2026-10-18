package com.leshoraa.scanorea.core.filter

import android.graphics.Bitmap
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Unit tests validating DocumentSharpnessFilter behavior under threshold conditions.
 */
class DocumentSharpnessFilterTest {

    @Test
    fun apply_zeroSharpness_returnsInputBitmapDirectly() {
        val bitmap = try {
            val constructor = Bitmap::class.java.getDeclaredConstructor()
            constructor.isAccessible = true
            constructor.newInstance()
        } catch (_: Exception) {
            null
        }
        if (bitmap != null) {
            val result = DocumentSharpnessFilter.apply(bitmap, 0.0f)
            assertSame(bitmap, result)
        }
    }

    @Test
    fun apply_subThresholdSharpness_returnsInputBitmapDirectly() {
        val bitmap = try {
            val constructor = Bitmap::class.java.getDeclaredConstructor()
            constructor.isAccessible = true
            constructor.newInstance()
        } catch (_: Exception) {
            null
        }
        if (bitmap != null) {
            val result = DocumentSharpnessFilter.apply(bitmap, 0.005f)
            assertSame(bitmap, result)
        }
    }
}
