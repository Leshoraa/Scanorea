package com.leshoraa.scanorea.features.presets.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateTokenizerTest {

    private val validKeys = setOf("DATE", "DD-MM-YYYY", "PROJECT", "CLIENT", "INVOICE")

    @Test
    fun parse_whenEmpty_returnsSingleEmptyLiteral() {
        val segments = TemplateTokenizer.parse("", validKeys)
        assertEquals(1, segments.size)
        assertTrue(segments[0] is RuleSegment.Literal)
        assertEquals("", (segments[0] as RuleSegment.Literal).text)
        assertEquals("", TemplateTokenizer.reconstruct(segments))
    }

    @Test
    fun parse_whenLiteralOnly_returnsSingleLiteral() {
        val segments = TemplateTokenizer.parse("Document_Name", validKeys)
        assertEquals(1, segments.size)
        assertEquals("Document_Name", (segments[0] as RuleSegment.Literal).text)
        assertEquals("Document_Name", TemplateTokenizer.reconstruct(segments))
    }

    @Test
    fun parse_whenSingleToken_returnsLiteralTokenLiteral() {
        val segments = TemplateTokenizer.parse("Doc_{DATE}", validKeys)
        assertEquals(3, segments.size)
        assertEquals("Doc_", (segments[0] as RuleSegment.Literal).text)
        assertEquals("DATE", (segments[1] as RuleSegment.Token).tagKey)
        assertEquals("", (segments[2] as RuleSegment.Literal).text)
        assertEquals("Doc_{DATE}", TemplateTokenizer.reconstruct(segments))
    }

    @Test
    fun parse_whenMultipleTokens_returnsAlternatingSegments() {
        val segments = TemplateTokenizer.parse("Doc_{DATE}_{PROJECT}_final", validKeys)
        assertEquals(5, segments.size)
        assertEquals("Doc_", (segments[0] as RuleSegment.Literal).text)
        assertEquals("DATE", (segments[1] as RuleSegment.Token).tagKey)
        assertEquals("_", (segments[2] as RuleSegment.Literal).text)
        assertEquals("PROJECT", (segments[3] as RuleSegment.Token).tagKey)
        assertEquals("_final", (segments[4] as RuleSegment.Literal).text)
        assertEquals("Doc_{DATE}_{PROJECT}_final", TemplateTokenizer.reconstruct(segments))
    }

    @Test
    fun parse_whenTokenNotValid_leavesItInLiteral() {
        val segments = TemplateTokenizer.parse("Doc_{UNKNOWN}_{DATE}", validKeys)
        assertEquals(3, segments.size)
        assertEquals("Doc_{UNKNOWN}_", (segments[0] as RuleSegment.Literal).text)
        assertEquals("DATE", (segments[1] as RuleSegment.Token).tagKey)
        assertEquals("", (segments[2] as RuleSegment.Literal).text)
        assertEquals("Doc_{UNKNOWN}_{DATE}", TemplateTokenizer.reconstruct(segments))
    }

    @Test
    fun deleteTokenAtIndex_whenFirstToken_mergesSurroundingLiterals() {
        val segments = TemplateTokenizer.parse("Doc_{DATE}_v1", validKeys)
        // [Literal("Doc_"), Token("{DATE}"), Literal("_v1")]
        val updated = TemplateTokenizer.deleteTokenAtIndex(segments, 1)
        assertEquals(1, updated.size)
        assertEquals("Doc__v1", (updated[0] as RuleSegment.Literal).text)
        assertEquals("Doc__v1", TemplateTokenizer.reconstruct(updated))
    }

    @Test
    fun deleteTokenAtIndex_whenMiddleToken_removesTokenCleanly() {
        val segments = TemplateTokenizer.parse("Doc_{DATE}_{PROJECT}_v1", validKeys)
        // 0: Literal("Doc_"), 1: Token("DATE"), 2: Literal("_"), 3: Token("PROJECT"), 4: Literal("_v1")
        val updated = TemplateTokenizer.deleteTokenAtIndex(segments, 3)
        assertEquals(3, updated.size)
        assertEquals("Doc_", (updated[0] as RuleSegment.Literal).text)
        assertEquals("DATE", (updated[1] as RuleSegment.Token).tagKey)
        assertEquals("__v1", (updated[2] as RuleSegment.Literal).text)
        assertEquals("Doc_{DATE}__v1", TemplateTokenizer.reconstruct(updated))
    }

    @Test
    fun deleteTokenAtIndex_whenOnlyToken_returnsSingleEmptyLiteral() {
        val segments = TemplateTokenizer.parse("{DATE}", validKeys)
        val updated = TemplateTokenizer.deleteTokenAtIndex(segments, 1)
        assertEquals(1, updated.size)
        assertEquals("", (updated[0] as RuleSegment.Literal).text)
        assertEquals("", TemplateTokenizer.reconstruct(updated))
    }
}
