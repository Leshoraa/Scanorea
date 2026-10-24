package com.leshoraa.scanorea.features.presets.domain

sealed class RuleSegment {
    data class Literal(val text: String) : RuleSegment()
    data class Token(val raw: String, val tagKey: String) : RuleSegment()
}

/**
 * Parses and manipulates filename template strings into atomic tokens and editable literal segments.
 *
 * Recognized tokens matching [validKeysUpper] are converted to [RuleSegment.Token], while
 * any surrounding or non-matching text remains in [RuleSegment.Literal].
 */
object TemplateTokenizer {

    private val TOKEN_REGEX = Regex("""\{([A-Za-z0-9_\-\.:]+)\}""")

    fun parse(template: String, validKeysUpper: Set<String>): List<RuleSegment> {
        if (template.isEmpty()) {
            return listOf(RuleSegment.Literal(""))
        }

        val segments = mutableListOf<RuleSegment>()
        var lastIndex = 0

        for (match in TOKEN_REGEX.findAll(template)) {
            val innerKey = match.groupValues[1]
            if (validKeysUpper.contains(innerKey.uppercase())) {
                val literalText = template.substring(lastIndex, match.range.first)
                segments.add(RuleSegment.Literal(literalText))
                segments.add(RuleSegment.Token(raw = match.value, tagKey = innerKey))
                lastIndex = match.range.last + 1
            }
        }

        val trailingText = template.substring(lastIndex)
        segments.add(RuleSegment.Literal(trailingText))

        return segments
    }

    fun reconstruct(segments: List<RuleSegment>): String {
        val sb = StringBuilder()
        for (seg in segments) {
            when (seg) {
                is RuleSegment.Literal -> sb.append(seg.text)
                is RuleSegment.Token -> sb.append(seg.raw)
            }
        }
        return sb.toString()
    }

    fun deleteTokenAtIndex(segments: List<RuleSegment>, tokenIndex: Int): List<RuleSegment> {
        if (tokenIndex !in segments.indices) return segments
        if (segments[tokenIndex] !is RuleSegment.Token) return segments

        val prevLiteral = (segments.getOrNull(tokenIndex - 1) as? RuleSegment.Literal)?.text.orEmpty()
        val nextLiteral = (segments.getOrNull(tokenIndex + 1) as? RuleSegment.Literal)?.text.orEmpty()
        val mergedLiteral = RuleSegment.Literal(prevLiteral + nextLiteral)

        val newSegments = mutableListOf<RuleSegment>()
        val startIndex = (tokenIndex - 1).coerceAtLeast(0)
        for (i in 0 until startIndex) {
            newSegments.add(segments[i])
        }
        newSegments.add(mergedLiteral)
        val endIndex = (tokenIndex + 2).coerceAtMost(segments.size)
        for (i in endIndex until segments.size) {
            newSegments.add(segments[i])
        }
        return newSegments
    }

    fun updateLiteralAtIndex(segments: List<RuleSegment>, literalIndex: Int, newText: String): List<RuleSegment> {
        if (literalIndex !in segments.indices) return segments
        if (segments[literalIndex] !is RuleSegment.Literal) return segments

        val newSegments = segments.toMutableList()
        newSegments[literalIndex] = RuleSegment.Literal(newText)
        return newSegments
    }
}
