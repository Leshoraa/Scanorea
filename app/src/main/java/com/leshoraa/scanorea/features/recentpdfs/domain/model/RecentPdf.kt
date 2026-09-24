package com.leshoraa.scanorea.features.recentpdfs.domain.model

import java.io.File

/**
 * Model representing a recently generated or saved PDF document,
 * enriched with organization metadata (category/folder, favorite status, tags).
 */
data class RecentPdf(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val category: String? = null,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList()
)
