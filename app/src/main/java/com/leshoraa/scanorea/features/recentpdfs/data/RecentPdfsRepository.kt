package com.leshoraa.scanorea.features.recentpdfs.data

import android.content.Context
import android.content.SharedPreferences
import com.leshoraa.scanorea.features.recentpdfs.domain.model.RecentPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Deep repository responsible for discovering, managing, and organizing
 * generated PDF documents with categories/folders, favorites, and tags.
 */
class RecentPdfsRepository(
    private val context: Context,
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
) {

    companion object {
        private const val PREFS_NAME = "scanorea_document_metadata"
        private const val KEY_CATEGORIES = "document_categories"
        private const val KEY_METADATA_MAP = "document_metadata_map"

        val DEFAULT_CATEGORIES = listOf("Work", "Study", "Receipts")
    }

    suspend fun getCategories(): List<String> = withContext(Dispatchers.IO) {
        val raw = prefs.getString(KEY_CATEGORIES, null)
        if (raw.isNullOrBlank()) {
            saveCategoriesList(DEFAULT_CATEGORIES)
            return@withContext DEFAULT_CATEGORIES
        }
        try {
            val arr = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val item = arr.getString(i).trim()
                if (item.isNotEmpty() && !list.contains(item)) {
                    list.add(item)
                }
            }
            if (list.isEmpty()) {
                saveCategoriesList(DEFAULT_CATEGORIES)
                DEFAULT_CATEGORIES
            } else {
                list
            }
        } catch (_: Exception) {
            DEFAULT_CATEGORIES
        }
    }

    suspend fun addCategory(category: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = category.trim()
        if (trimmed.isEmpty() || trimmed.equals("All", ignoreCase = true) || trimmed.equals("Favorites", ignoreCase = true)) {
            return@withContext false
        }
        val current = getCategories().toMutableList()
        if (current.any { it.equals(trimmed, ignoreCase = true) }) {
            return@withContext false
        }
        current.add(trimmed)
        saveCategoriesList(current)
        true
    }

    suspend fun deleteCategory(category: String): Boolean = withContext(Dispatchers.IO) {
        val current = getCategories().toMutableList()
        val removed = current.removeAll { it.equals(category.trim(), ignoreCase = true) }
        if (removed) {
            saveCategoriesList(current)
            val metadataMap = getMetadataMap()
            var modified = false
            for (key in metadataMap.keys()) {
                val docObj = metadataMap.getJSONObject(key)
                if (docObj.optString("category").equals(category.trim(), ignoreCase = true)) {
                    docObj.remove("category")
                    modified = true
                }
            }
            if (modified) {
                saveMetadataMap(metadataMap)
            }
        }
        removed
    }

    suspend fun getRecentPdfs(): List<RecentPdf> = withContext(Dispatchers.IO) {
        val pdfDir = File(context.filesDir, "pdfs")
        if (!pdfDir.exists() || !pdfDir.isDirectory) {
            return@withContext emptyList()
        }

        val metadataMap = getMetadataMap()
        val pdfFiles = pdfDir.listFiles { file ->
            file.isFile && file.name.endsWith(".pdf", ignoreCase = true)
        } ?: return@withContext emptyList()

        val existingFileNames = pdfFiles.map { it.name }.toSet()

        val pdfList = pdfFiles.map { file ->
            val docObj = metadataMap.optJSONObject(file.name)
            val category = docObj?.optString("category")?.takeIf { it.isNotBlank() }
            val isFavorite = docObj?.optBoolean("isFavorite", false) ?: false
            val tagsList = mutableListOf<String>()
            val tagsArr = docObj?.optJSONArray("tags")
            if (tagsArr != null) {
                for (i in 0 until tagsArr.length()) {
                    val t = tagsArr.optString(i).trim()
                    if (t.isNotEmpty()) tagsList.add(t)
                }
            }

            RecentPdf(
                file = file,
                name = file.name,
                sizeBytes = file.length(),
                lastModifiedMillis = file.lastModified(),
                category = category,
                isFavorite = isFavorite,
                tags = tagsList
            )
        }.sortedByDescending { it.lastModifiedMillis }

        // Clean up metadata keys for removed files
        val keysToRemove = mutableListOf<String>()
        for (key in metadataMap.keys()) {
            if (!existingFileNames.contains(key)) {
                keysToRemove.add(key)
            }
        }
        if (keysToRemove.isNotEmpty()) {
            keysToRemove.forEach { metadataMap.remove(it) }
            saveMetadataMap(metadataMap)
        }

        pdfList
    }

    suspend fun updatePdfCategory(file: File, category: String?): Boolean = withContext(Dispatchers.IO) {
        val metadataMap = getMetadataMap()
        val docObj = metadataMap.optJSONObject(file.name) ?: JSONObject()
        val trimmed = category?.trim()
        if (trimmed.isNullOrEmpty()) {
            docObj.remove("category")
        } else {
            docObj.put("category", trimmed)
        }
        metadataMap.put(file.name, docObj)
        saveMetadataMap(metadataMap)
        true
    }

    suspend fun toggleFavorite(file: File): Boolean = withContext(Dispatchers.IO) {
        val metadataMap = getMetadataMap()
        val docObj = metadataMap.optJSONObject(file.name) ?: JSONObject()
        val newFav = !(docObj.optBoolean("isFavorite", false))
        docObj.put("isFavorite", newFav)
        metadataMap.put(file.name, docObj)
        saveMetadataMap(metadataMap)
        newFav
    }

    suspend fun updatePdfTags(file: File, tags: List<String>): Boolean = withContext(Dispatchers.IO) {
        val metadataMap = getMetadataMap()
        val docObj = metadataMap.optJSONObject(file.name) ?: JSONObject()
        val tagsArr = JSONArray()
        tags.forEach { tagsArr.put(it.trim()) }
        docObj.put("tags", tagsArr)
        metadataMap.put(file.name, docObj)
        saveMetadataMap(metadataMap)
        true
    }

    suspend fun deletePdf(file: File): Boolean = withContext(Dispatchers.IO) {
        val metadataMap = getMetadataMap()
        if (metadataMap.has(file.name)) {
            metadataMap.remove(file.name)
            saveMetadataMap(metadataMap)
        }

        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    private fun getMetadataMap(): JSONObject {
        val raw = prefs.getString(KEY_METADATA_MAP, null) ?: return JSONObject()
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    private fun saveMetadataMap(json: JSONObject) {
        prefs.edit().putString(KEY_METADATA_MAP, json.toString()).apply()
    }

    private fun saveCategoriesList(list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(KEY_CATEGORIES, arr.toString()).apply()
    }
}
