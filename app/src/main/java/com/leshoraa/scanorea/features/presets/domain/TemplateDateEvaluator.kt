package com.leshoraa.scanorea.features.presets.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility evaluating dynamic date & custom user-defined tags in document name templates.
 *
 * Supported Tokens:
 * - Dynamic Dates: `{DD:MM:YYYY}`, `{DD-MM-YYYY}`, `{DD.MM.YYYY}`, `{YYYY-MM-DD}`, `{DATE}`, `{YYYY}`, `{MM}`, `{DD}`
 * - User-Defined Custom Tags: Any `{TAG_NAME}` defined by the user (e.g. `{PROJECT}`, `{CLIENT}`, `{NIM}`, `{NAME}`)
 *
 * Example:
 * `Doc_{PROJECT}_{DD-MM-YYYY}` -> `Doc_Scanorea_23-10-2026`
 */
object TemplateDateEvaluator {

    @Volatile
    private var customTagsMap: Map<String, String> = emptyMap()

    fun setCustomTags(tags: Map<String, String>) {
        customTagsMap = tags
    }

    fun getCustomTags(): Map<String, String> = customTagsMap

    /**
     * Backward-compatible helper for student credentials.
     */
    fun initStudentProfile(nim: String, name: String, subject: String = "") {
        val updated = customTagsMap.toMutableMap()
        if (nim.isNotBlank()) updated["NIM"] = nim.trim() else updated.remove("NIM")
        if (name.isNotBlank()) updated["NAMA"] = name.trim() else updated.remove("NAMA")
        if (subject.isNotBlank()) updated["MATKUL"] = subject.trim() else updated.remove("MATKUL")
        customTagsMap = updated
    }

    fun evaluate(
        template: String,
        currentDate: Date = Date(),
        overrideTags: Map<String, String>? = null,
        overrideNim: String? = null,
        overrideName: String? = null,
        overrideSubject: String? = null
    ): String {
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

        // Merge active tags
        val activeTags = (overrideTags ?: customTagsMap).toMutableMap()
        overrideNim?.let { if (it.isNotBlank()) activeTags["NIM"] = it.trim() }
        overrideName?.let { if (it.isNotBlank()) activeTags["NAMA"] = it.trim() }
        overrideSubject?.let { if (it.isNotBlank()) activeTags["MATKUL"] = it.trim() }

        // Replace user-defined custom tags
        for ((key, value) in activeTags) {
            val resolvedVal = value.ifBlank { key }
            result = result.replace(Regex("(?i)\\{" + Regex.escape(key) + "\\}"), resolvedVal)
        }

        // Built-in generic fallbacks if specific tag not defined
        if (!activeTags.containsKey("NIM")) {
            result = result.replace(Regex("(?i)\\{NIM\\}"), "NIM")
        }
        if (!activeTags.containsKey("NAMA") && !activeTags.containsKey("NAME")) {
            result = result.replace(Regex("(?i)\\{NAMA\\}"), "Nama")
            result = result.replace(Regex("(?i)\\{NAME\\}"), "Nama")
        }
        if (!activeTags.containsKey("MATKUL") && !activeTags.containsKey("SUBJECT")) {
            result = result.replace(Regex("(?i)\\{MATKUL\\}"), "Tugas")
            result = result.replace(Regex("(?i)\\{SUBJECT\\}"), "Tugas")
        }
        if (!activeTags.containsKey("TUGAS") && !activeTags.containsKey("TASK")) {
            result = result.replace(Regex("(?i)\\{TUGAS\\}"), "Tugas")
            result = result.replace(Regex("(?i)\\{TASK\\}"), "Tugas")
        }

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
