package com.leshoraa.scanorea.features.recentpdfs.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for reactive queries and atomic updates on indexed PDFs,
 * categories, and pinned folders.
 */
@Dao
interface RecentPdfsDao {

    // --- PDF Documents ---

    @Query("SELECT * FROM recent_pdfs ORDER BY lastModified DESC")
    fun getAllPdfsFlow(): Flow<List<RecentPdfEntity>>

    @Query("SELECT * FROM recent_pdfs ORDER BY lastModified DESC")
    suspend fun getAllPdfs(): List<RecentPdfEntity>

    @Query("SELECT * FROM recent_pdfs WHERE filePath = :filePath LIMIT 1")
    suspend fun getPdfByPath(filePath: String): RecentPdfEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPdf(entity: RecentPdfEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPdfs(entities: List<RecentPdfEntity>)

    @Query("DELETE FROM recent_pdfs WHERE filePath = :filePath")
    suspend fun deletePdfByPath(filePath: String): Int

    @Query("DELETE FROM recent_pdfs WHERE filePath NOT IN (:activeFilePaths)")
    suspend fun deletePdfsNotIn(activeFilePaths: List<String>): Int

    @Query("UPDATE recent_pdfs SET isFavorite = :isFavorite WHERE filePath = :filePath")
    suspend fun updateFavorite(filePath: String, isFavorite: Boolean): Int

    @Query("UPDATE recent_pdfs SET foldersJson = :foldersJson WHERE filePath = :filePath")
    suspend fun updateFolders(filePath: String, foldersJson: String): Int

    // --- Categories / Folders ---

    @Query("SELECT * FROM categories ORDER BY orderIndex ASC")
    fun getCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY orderIndex ASC")
    suspend fun getCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun deleteCategory(name: String): Int

    @Query("DELETE FROM categories")
    suspend fun clearCategories(): Int

    // --- Pinned Folders ---

    @Query("SELECT * FROM pinned_folders ORDER BY orderIndex ASC")
    fun getPinnedFoldersFlow(): Flow<List<PinnedFolderEntity>>

    @Query("SELECT * FROM pinned_folders ORDER BY orderIndex ASC")
    suspend fun getPinnedFolders(): List<PinnedFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPinnedFolders(folders: List<PinnedFolderEntity>)

    @Query("DELETE FROM pinned_folders")
    suspend fun clearPinnedFolders(): Int
}
