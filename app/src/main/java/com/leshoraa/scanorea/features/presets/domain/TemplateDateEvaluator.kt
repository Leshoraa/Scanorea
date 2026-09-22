package com.leshoraa.scanorea.features.presets.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility evaluating dynamic date tokens in document name templates.
 *
 * Example:
 * `Rendra_23.XX.XXXX_Aljabar_{DD:MM:YYYY}` -> `Rendra_23.XX.XXXX_Aljabar_21-09-2026`
 */
object TemplateDateEvaluator {

    fun evaluate(template: String, currentDate: Date = Date()): String {
        if (template.isBlank()) return template

        val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MM", Locale.getDefault())
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        val dayMonthYearHyphen = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val dayMonthYearDot = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val yearMonthDayHyphen = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val day = dayFormat.format(currentDate)
        val month = monthFormat.format(currentDate)
        val year = yearFormat.format(currentDate)
        val dmyHyphen = dayMonthYearHyphen.format(currentDate)
        val dmyDot = dayMonthYearDot.format(currentDate)
        val ymdHyphen = yearMonthDayHyphen.format(currentDate)

        var result = template
        // Note: Colons ':' are invalid in Android/Linux file paths, so {DD:MM:YYYY} resolves cleanly to hyphenated date
        result = result.replace(Regex("(?i)\\{DD[:_]MM[:_]YYYY\\}"), dmyHyphen)
        result = result.replace(Regex("(?i)\\{DD-MM-YYYY\\}"), dmyHyphen)
        result = result.replace(Regex("(?i)\\{DD\\.MM\\.YYYY\\}"), dmyDot)
        result = result.replace(Regex("(?i)\\{YYYY[:_-]MM[:_-]DD\\}"), ymdHyphen)
        result = result.replace(Regex("(?i)\\{DATE\\}"), ymdHyphen)
        result = result.replace(Regex("(?i)\\{YYYY\\}"), year)
        result = result.replace(Regex("(?i)\\{MM\\}"), month)
        result = result.replace(Regex("(?i)\\{DD\\}"), day)

        return result
    }
}
