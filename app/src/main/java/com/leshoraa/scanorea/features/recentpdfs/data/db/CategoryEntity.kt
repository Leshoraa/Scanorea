package com.leshoraa.scanorea.features.recentpdfs.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a user-defined category / folder.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val name: String,
    val orderIndex: Int
)
