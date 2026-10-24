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
        val template = "Report_{DD:MM:YYYY}"
        val evaluated = TemplateDateEvaluator.evaluate(template, fixedDate)
        assertEquals("Report_21-09-2026", evaluated)
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

    @Test
    fun evaluate_whenTemplateHasCustomTags_resolvesTags() {
        val tags = mapOf("PROJECT" to "Alpha", "CLIENT" to "AcmeCorp")
        val template = "{PROJECT}_{CLIENT}_{DD-MM-YYYY}"
        val evaluated = TemplateDateEvaluator.evaluate(template, fixedDate, overrideTags = tags)
        assertEquals("Alpha_AcmeCorp_21-09-2026", evaluated)
    }

    @Test
    fun evaluate_whenGlobalCustomTagsSet_resolvesCorrectly() {
        TemplateDateEvaluator.setCustomTags(mapOf("AUTHOR" to "JohnDoe"))
        val template = "{AUTHOR}_Doc_{DATE}"
        val evaluated = TemplateDateEvaluator.evaluate(template, fixedDate)
        assertEquals("JohnDoe_Doc_2026-09-21", evaluated)
    }
}
