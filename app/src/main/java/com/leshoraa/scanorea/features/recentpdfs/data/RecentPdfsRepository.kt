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

        val DEFAULT_CATEGORIES = listOf("Work", "Study")
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
                // Sanitize out old Receipts default and ensure no duplicates
                if (item.isNotEmpty() && !item.equals("Receipts", ignoreCase = true) && !list.contains(item)) {
                    list.add(item)
                }
            }
            if (list.isEmpty()) {
                saveCategoriesList(DEFAULT_CATEGORIES)
                DEFAULT_CATEGORIES
            } else {
                saveCategoriesList(list)
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

    suspend fun renameCategory(oldCategory: String, newCategory: String): Boolean = withContext(Dispatchers.IO) {
        val oldTrimmed = oldCategory.trim()
        val newTrimmed = newCategory.trim()
        if (newTrimmed.isEmpty() ||
            newTrimmed.equals("All", ignoreCase = true) ||
            newTrimmed.equals("Favorites", ignoreCase = true)
        ) {
            return@withContext false
        }

        val current = getCategories().toMutableList()
        val index = current.indexOfFirst { it.equals(oldTrimmed, ignoreCase = true) }
        if (index == -1) return@withContext false

        // Check if new name conflicts with another category
        val conflict = current.indices.any { it != index && current[it].equals(newTrimmed, ignoreCase = true) }
        if (conflict) return@withContext false

        current[index] = newTrimmed
        saveCategoriesList(current)

        // Update all documents assigned to oldCategory
        val metadataMap = getMetadataMap()
        var modified = false
        for (key in metadataMap.keys()) {
            val docObj = metadataMap.getJSONObject(key)
            var docModified = false

            // Update folders array if present
            val foldersArr = docObj.optJSONArray("folders")
            if (foldersArr != null) {
                val updatedArr = JSONArray()
                for (i in 0 until foldersArr.length()) {
                    val f = foldersArr.optString(i)
                    if (f.equals(oldTrimmed, ignoreCase = true)) {
                        updatedArr.put(newTrimmed)
                        docModified = true
                    } else {
                        updatedArr.put(f)
                    }
                }
                if (docModified) {
                    docObj.put("folders", updatedArr)
                }
            }

            if (docObj.optString("category").equals(oldTrimmed, ignoreCase = true)) {
                docObj.put("category", newTrimmed)
                docModified = true
            }

            if (docModified) {
                modified = true
            }
        }
        if (modified) {
            saveMetadataMap(metadataMap)
        }

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
                var docModified = false

                val foldersArr = docObj.optJSONArray("folders")
                if (foldersArr != null) {
                    val updatedArr = JSONArray()
                    for (i in 0 until foldersArr.length()) {
                        val f = foldersArr.optString(i)
                        if (!f.equals(category.trim(), ignoreCase = true)) {
                            updatedArr.put(f)
                        } else {
                            docModified = true
                        }
                    }
                    if (docModified) {
                        docObj.put("folders", updatedArr)
                    }
                }

                if (docObj.optString("category").equals(category.trim(), ignoreCase = true)) {
                    val remainingFirst = docObj.optJSONArray("folders")?.optString(0)?.takeIf { it.isNotBlank() }
                    if (remainingFirst != null) {
                        docObj.put("category", remainingFirst)
                    } else {
                        docObj.remove("category")
                    }
                    docModified = true
                }

                if (docModified) {
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
            val foldersList = mutableListOf<String>()
            val foldersArr = docObj?.optJSONArray("folders")
            if (foldersArr != null) {
                for (i in 0 until foldersArr.length()) {
                    val f = foldersArr.optString(i).trim()
                    if (f.isNotEmpty() && !foldersList.contains(f)) {
                        foldersList.add(f)
                    }
                }
            } else {
                val legacyCategory = docObj?.optString("category")?.trim()
                if (!legacyCategory.isNullOrEmpty()) {
                    foldersList.add(legacyCategory)
                }
            }

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
                folders = foldersList,
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

    suspend fun updatePdfFolders(file: File, folders: List<String>): Boolean = withContext(Dispatchers.IO) {
        val metadataMap = getMetadataMap()
        val docObj = metadataMap.optJSONObject(file.name) ?: JSONObject()
        val distinctFolders = folders.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val foldersArr = JSONArray()
        distinctFolders.forEach { foldersArr.put(it) }
        docObj.put("folders", foldersArr)
        if (distinctFolders.isNotEmpty()) {
            docObj.put("category", distinctFolders.first())
        } else {
            docObj.remove("category")
        }
        metadataMap.put(file.name, docObj)
        saveMetadataMap(metadataMap)
        true
    }

    suspend fun updatePdfCategory(file: File, category: String?): Boolean = withContext(Dispatchers.IO) {
        val folderList = if (category.isNullOrBlank()) emptyList() else listOf(category.trim())
        updatePdfFolders(file, folderList)
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
