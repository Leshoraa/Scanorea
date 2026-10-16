package com.leshoraa.scanorea.features.recentpdfs.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an indexed PDF document and its user metadata.
 */
@Entity(
    tableName = "recent_pdfs",
    indices = [
        Index(value = ["lastModified"]),
        Index(value = ["isFavorite"])
    ]
)
data class RecentPdfEntity(
    @PrimaryKey
    val filePath: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val lastModified: Long,
    val pageCount: Int,
    val isFavorite: Boolean,
    val foldersJson: String,
    val tagsJson: String,
    val customTitle: String?
)
