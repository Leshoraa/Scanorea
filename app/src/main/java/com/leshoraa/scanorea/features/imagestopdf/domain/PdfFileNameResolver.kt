package com.leshoraa.scanorea.features.imagestopdf.domain

/**
 * Pure helper responsible for turning a user-supplied display name into a safe,
 * unique PDF file name.
 *
 * Keeping this logic free of [java.io.File] makes it trivially unit-testable on the JVM
 * (no Android framework, no filesystem).
 */
object PdfFileNameResolver {

    /** Hard ceiling for the base name (excluding extension and uniqueness suffix). */
    const val MAX_BASE_NAME_LENGTH: Int = 90

    private const val PDF_EXTENSION = ".pdf"

    private val INVALID_CHARACTERS = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]")

    /**
     * Sanitizes [desiredName] into a valid `*.pdf` file name.
     *
     * Invalid filesystem characters are replaced with `_`. Blank names, names consisting
     * only of dots (e.g. `".."`), or a bare extension (`".pdf"`) fall back to
     * `Scanorea_<fallbackTimestampMillis>.pdf`.
     */
    fun sanitize(desiredName: String, fallbackTimestampMillis: Long): String {
        val trimmed = desiredName.trim()
        val withoutExtension = if (trimmed.endsWith(PDF_EXTENSION, ignoreCase = true)) {
            trimmed.dropLast(PDF_EXTENSION.length)
        } else {
            trimmed
        }

        val cleaned = withoutExtension.replace(INVALID_CHARACTERS, "_").trim()
        val baseName = if (cleaned.isBlank() || cleaned.all { it == '.' }) {
            "Scanorea_$fallbackTimestampMillis"
        } else {
            cleaned.take(MAX_BASE_NAME_LENGTH).trim().ifBlank { "Scanorea_$fallbackTimestampMillis" }
        }

        return "$baseName$PDF_EXTENSION"
    }

    /**
     * Returns a file name based on [desiredName] that does not collide with any entry in
     * [existingNames] (case-insensitive comparison, matching how FAT/Windows compare names).
     *
     * Collisions are resolved by appending ` (1)`, ` (2)`, ... before the extension so that
     * an existing document is never overwritten.
     */
    fun resolveUnique(
        desiredName: String,
        existingNames: Collection<String>,
        fallbackTimestampMillis: Long
    ): String {
        val sanitized = sanitize(desiredName, fallbackTimestampMillis)
        val taken = existingNames
            .asSequence()
            .filter { it.isNotBlank() }
            .map { it.trim().lowercase() }
            .toHashSet()

        if (sanitized.lowercase() !in taken) return sanitized

        val baseName = sanitized.dropLast(PDF_EXTENSION.length)
        var index = 1
        while (true) {
            val candidate = "$baseName ($index)$PDF_EXTENSION"
            if (candidate.lowercase() !in taken) return candidate
            index++
        }
    }
}
