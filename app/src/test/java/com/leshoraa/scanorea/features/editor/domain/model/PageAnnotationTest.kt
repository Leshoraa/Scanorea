package com.leshoraa.scanorea.features.editor.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying PageAnnotation models, tools, and color configurations.
 */
class PageAnnotationTest {

    @Test
    fun freehandPath_initializesWithCorrectToolAndDefaults() {
        val points = listOf(NormalizedPoint(0.1f, 0.1f), NormalizedPoint(0.2f, 0.2f))
        val path = PageAnnotation.FreehandPath(
            color = AnnotationColors.YELLOW,
            points = points
        )

        assertEquals(AnnotationTool.PEN, path.tool)
        assertEquals(AnnotationColors.YELLOW, path.color)
        assertEquals(1.0f, path.alpha)
        assertEquals(2, path.points.size)
        assertNotNull(path.id)
    }

    @Test
    fun rectBox_toolResolution_correctlyIdentifiesTool() {
        val highlighter = PageAnnotation.RectBox(
            color = AnnotationColors.YELLOW,
            left = 0f, top = 0f, right = 0.5f, bottom = 0.5f,
            isFilled = true,
            alpha = 0.4f
        )
        assertEquals(AnnotationTool.HIGHLIGHTER, highlighter.tool)
        assertTrue(highlighter.isFilled)
        assertEquals(0.4f, highlighter.alpha)

        val redact = PageAnnotation.RectBox(
            color = AnnotationColors.BLACK,
            left = 0f, top = 0f, right = 0.5f, bottom = 0.5f,
            isFilled = true,
            alpha = 1.0f
        )
        assertEquals(AnnotationTool.REDACT, redact.tool)

        val rectShape = PageAnnotation.RectBox(
            color = AnnotationColors.BLUE,
            left = 0f, top = 0f, right = 0.5f, bottom = 0.5f,
            isFilled = false,
            alpha = 0.8f
        )
        assertEquals(AnnotationTool.RECT_SHAPE, rectShape.tool)
        assertFalse(rectShape.isFilled)
        assertEquals(0.8f, rectShape.alpha)
    }

    @Test
    fun ovalShape_initializesWithCorrectToolAndAlpha() {
        val oval = PageAnnotation.OvalShape(
            color = AnnotationColors.GREEN,
            left = 0.2f, top = 0.2f, right = 0.8f, bottom = 0.6f,
            alpha = 0.75f
        )

        assertEquals(AnnotationTool.OVAL_SHAPE, oval.tool)
        assertEquals(0.75f, oval.alpha)
        assertEquals(AnnotationColors.GREEN, oval.color)
    }

    @Test
    fun arrowLine_initializesWithCorrectToolAndCoordinates() {
        val arrow = PageAnnotation.ArrowLine(
            color = AnnotationColors.RED,
            startX = 0.1f, startY = 0.1f,
            endX = 0.9f, endY = 0.9f,
            alpha = 0.9f
        )

        assertEquals(AnnotationTool.ARROW_SHAPE, arrow.tool)
        assertEquals(0.9f, arrow.alpha)
        assertEquals(0.1f, arrow.startX)
        assertEquals(0.9f, arrow.endX)
    }

    @Test
    fun presets_containsExpectedColors() {
        val presets = AnnotationColors.PRESETS
        assertEquals(8, presets.size)
        assertTrue(presets.contains(AnnotationColors.YELLOW))
        assertTrue(presets.contains(AnnotationColors.RED))
        assertTrue(presets.contains(AnnotationColors.GREEN))
        assertTrue(presets.contains(AnnotationColors.BLUE))
        assertTrue(presets.contains(AnnotationColors.ORANGE))
        assertTrue(presets.contains(AnnotationColors.PURPLE))
        assertTrue(presets.contains(AnnotationColors.BLACK))
        assertTrue(presets.contains(AnnotationColors.WHITE))
    }

    @Test
    fun strokeSize_hasValidDimensionsAndLabels() {
        assertEquals("Thin", StrokeSize.THIN.label)
        assertEquals("Med", StrokeSize.MEDIUM.label)
        assertEquals("Thick", StrokeSize.THICK.label)
        assertTrue(StrokeSize.THIN.widthDp < StrokeSize.MEDIUM.widthDp)
        assertTrue(StrokeSize.MEDIUM.widthDp < StrokeSize.THICK.widthDp)
    }

    @Test
    fun annotationTool_containsNavigateOption() {
        assertEquals("Navigate", AnnotationTool.NAVIGATE.label)
        val tools = AnnotationTool.entries
        assertTrue(tools.contains(AnnotationTool.NAVIGATE))
        assertTrue(tools.contains(AnnotationTool.PEN))
    }

    @Test
    fun rotateClockwise_rotatesAllAnnotationTypes() {
        val path = PageAnnotation.FreehandPath(
            color = AnnotationColors.RED,
            points = listOf(NormalizedPoint(0.2f, 0.3f))
        ).rotateClockwise() as PageAnnotation.FreehandPath
        assertEquals(0.7f, path.points.first().x, 1e-4f)
        assertEquals(0.2f, path.points.first().y, 1e-4f)

        val rect = PageAnnotation.RectBox(
            color = AnnotationColors.BLUE,
            left = 0.1f, top = 0.2f, right = 0.6f, bottom = 0.8f,
            isFilled = false
        ).rotateClockwise() as PageAnnotation.RectBox
        assertEquals(0.2f, rect.left, 1e-4f)
        assertEquals(0.1f, rect.top, 1e-4f)
        assertEquals(0.8f, rect.right, 1e-4f)
        assertEquals(0.6f, rect.bottom, 1e-4f)

        val arrow = PageAnnotation.ArrowLine(
            color = AnnotationColors.GREEN,
            startX = 0.2f, startY = 0.3f, endX = 0.7f, endY = 0.8f
        ).rotateClockwise() as PageAnnotation.ArrowLine
        assertEquals(0.7f, arrow.startX, 1e-4f)
        assertEquals(0.2f, arrow.startY, 1e-4f)
        assertEquals(0.2f, arrow.endX, 1e-4f)
        assertEquals(0.7f, arrow.endY, 1e-4f)
    }

    @Test
    fun rotateCounterClockwise_rotatesAllAnnotationTypes() {
        val rect = PageAnnotation.RectBox(
            color = AnnotationColors.BLUE,
            left = 0.1f, top = 0.2f, right = 0.6f, bottom = 0.8f,
            isFilled = false
        )
        val reverted = rect.rotateClockwise().rotateCounterClockwise() as PageAnnotation.RectBox
        assertEquals(rect.left, reverted.left, 1e-4f)
        assertEquals(rect.top, reverted.top, 1e-4f)
        assertEquals(rect.right, reverted.right, 1e-4f)
        assertEquals(rect.bottom, reverted.bottom, 1e-4f)
    }

    @Test
    fun rotateClockwise_fourTimes_returnsOriginalCoordinates() {
        val rect = PageAnnotation.RectBox(
            color = AnnotationColors.BLUE,
            left = 0.15f, top = 0.25f, right = 0.65f, bottom = 0.75f,
            isFilled = false
        )
        val fullRotatedRect = rect
            .rotateClockwise()
            .rotateClockwise()
            .rotateClockwise()
            .rotateClockwise() as PageAnnotation.RectBox
        assertEquals(rect.left, fullRotatedRect.left, 1e-4f)
        assertEquals(rect.top, fullRotatedRect.top, 1e-4f)
        assertEquals(rect.right, fullRotatedRect.right, 1e-4f)
        assertEquals(rect.bottom, fullRotatedRect.bottom, 1e-4f)

        val arrow = PageAnnotation.ArrowLine(
            color = AnnotationColors.RED,
            startX = 0.2f, startY = 0.3f, endX = 0.8f, endY = 0.6f
        )
        val fullRotatedArrow = arrow
            .rotateClockwise()
            .rotateClockwise()
            .rotateClockwise()
            .rotateClockwise() as PageAnnotation.ArrowLine
        assertEquals(arrow.startX, fullRotatedArrow.startX, 1e-4f)
        assertEquals(arrow.startY, fullRotatedArrow.startY, 1e-4f)
        assertEquals(arrow.endX, fullRotatedArrow.endX, 1e-4f)
        assertEquals(arrow.endY, fullRotatedArrow.endY, 1e-4f)
    }

    @Test
    fun adjustmentTool_containsSharpness() {
        val tools = AdjustmentTool.entries
        assertTrue(tools.contains(AdjustmentTool.CONTRAST))
        assertTrue(tools.contains(AdjustmentTool.BRIGHTNESS))
        assertTrue(tools.contains(AdjustmentTool.SHARPNESS))
    }
}
