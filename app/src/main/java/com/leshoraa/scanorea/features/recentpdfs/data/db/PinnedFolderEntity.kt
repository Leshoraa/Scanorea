package com.leshoraa.scanorea.features.recentpdfs.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing pinned folder chips on the Home grid.
 */
@Entity(tableName = "pinned_folders")
data class PinnedFolderEntity(
    @PrimaryKey
    val name: String,
    val orderIndex: Int
)
