package com.leshoraa.scanorea.features.recentpdfs.domain.model

import java.io.File

/**
 * Model representing a recently generated or saved PDF document,
 * enriched with organization metadata (folders, favorite status, tags).
 * A document can belong to multiple folders simultaneously.
 */
data class RecentPdf(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val folders: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList()
) {
    /** Primary category for backwards compatibility */
    val category: String?
        get() = folders.firstOrNull()

    constructor(
        file: File,
        name: String,
        sizeBytes: Long,
        lastModifiedMillis: Long,
        category: String?,
        isFavorite: Boolean = false,
        tags: List<String> = emptyList()
    ) : this(
        file = file,
        name = name,
        sizeBytes = sizeBytes,
        lastModifiedMillis = lastModifiedMillis,
        folders = if (!category.isNullOrBlank()) listOf(category) else emptyList(),
        isFavorite = isFavorite,
        tags = tags
    )
}
