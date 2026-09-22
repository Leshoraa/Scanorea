package com.leshoraa.scanorea.features.presets.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Unit tests verifying [TemplateDateEvaluator] token resolution.
 */
class TemplateDateEvaluatorTest {

    private val fixedDate: Date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-09-21")!!

    @Test
    fun evaluate_whenTemplateHasDayMonthYearHyphen_resolvesCorrectly() {
        val template = "Rendra_23.XX.XXXX_Aljabar_{DD:MM:YYYY}"
        val evaluated = TemplateDateEvaluator.evaluate(template, fixedDate)
        assertEquals("Rendra_23.XX.XXXX_Aljabar_21-09-2026", evaluated)
    }

    @Test
    fun evaluate_whenTemplateHasDotDate_resolvesCorrectly() {
        val template = "Scan_{DD.MM.YYYY}"
        val evaluated = TemplateDateEvaluator.evaluate(template, fixedDate)
        assertEquals("Scan_21.09.2026", evaluated)
    }

    @Test
    fun evaluate_whenTemplateHasYearMonthDay_resolvesCorrectly() {
        val template = "Doc_{YYYY-MM-DD}"
        val evaluated = TemplateDateEvaluator.evaluate(template, fixedDate)
        assertEquals("Doc_2026-09-21", evaluated)
    }

    @Test
    fun evaluate_whenTemplateHasIndividualTokens_resolvesCorrectly() {
        val template = "Archive_{YYYY}_{MM}_{DD}"
        val evaluated = TemplateDateEvaluator.evaluate(template, fixedDate)
        assertEquals("Archive_2026_09_21", evaluated)
    }

    @Test
    fun evaluate_whenTemplateHasNoTokens_returnsUnchanged() {
        val template = "StaticDocumentName"
        val evaluated = TemplateDateEvaluator.evaluate(template, fixedDate)
        assertEquals("StaticDocumentName", evaluated)
    }
}
