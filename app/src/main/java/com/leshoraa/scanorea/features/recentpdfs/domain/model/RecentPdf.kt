package com.leshoraa.scanorea.features.recentpdfs.domain.model

import java.io.File

/**
 * Model representing a recently generated or saved PDF document.
 */
data class RecentPdf(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long
)
