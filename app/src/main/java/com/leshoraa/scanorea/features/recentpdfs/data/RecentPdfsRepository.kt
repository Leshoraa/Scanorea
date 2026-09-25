package com.leshoraa.scanorea.features.recentpdfs.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
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
        private const val TAG = "RecentPdfsRepository"
        private const val PREFS_NAME = "scanorea_document_metadata"
        private const val KEY_CATEGORIES = "document_categories"
        private const val KEY_PINNED_FOLDERS = "document_pinned_folders"
        private const val KEY_METADATA_MAP = "document_metadata_map"

        const val MAX_PINNED_FOLDERS = 3
        val DEFAULT_CATEGORIES = listOf("Work", "Study", "Personal")
        val DEFAULT_PINNED_FOLDERS = listOf("Favorites", "Work", "Study")
    }

    suspend fun getCategories(): List<String> = withContext(Dispatchers.IO) {
        val raw = prefs.getString(KEY_CATEGORIES, null)
        if (raw == null) {
            saveCategoriesList(DEFAULT_CATEGORIES)
            return@withContext DEFAULT_CATEGORIES
        }
        val items = deserializeStringList(raw)
        val list = mutableListOf<String>()
        for (item in items) {
            val trimmed = item.trim()
            if (trimmed.isNotEmpty() && !trimmed.equals("Receipts", ignoreCase = true) && !list.contains(trimmed)) {
                list.add(trimmed)
            }
        }
        list
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
        val keys = metadataMap.keys()
        if (keys != null) {
            for (key in keys) {
                val docObj = metadataMap.optJSONObject(key) ?: continue
                var docModified = false

                // Update folders array if present
                val foldersArr = docObj.optJSONArray("folders")
                if (foldersArr != null) {
                    val updatedArr = JSONArray()
                    for (i in 0 until foldersArr.length()) {
                        val folderName = foldersArr.optString(i)
                        if (folderName.equals(oldTrimmed, ignoreCase = true)) {
                            updatedArr.put(newTrimmed)
                            docModified = true
                        } else {
                            updatedArr.put(folderName)
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
        }
        if (modified) {
            saveMetadataMap(metadataMap)
        }

        // Keep pinned folders list in sync with renamed category
        val currentPinned = getPinnedFolders().toMutableList()
        val pinnedIndex = currentPinned.indexOfFirst { it.equals(oldTrimmed, ignoreCase = true) }
        if (pinnedIndex >= 0) {
            currentPinned[pinnedIndex] = newTrimmed
            savePinnedFoldersList(currentPinned)
        }

        true
    }

    suspend fun deleteCategory(category: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = category.trim()
        if (trimmed.isEmpty() || trimmed.equals("All", ignoreCase = true) || trimmed.equals("Favorites", ignoreCase = true)) {
            return@withContext false
        }

        val current = getCategories().toMutableList()
        val removedFromCategories = current.removeAll { it.equals(trimmed, ignoreCase = true) }
        if (removedFromCategories) {
            saveCategoriesList(current)
        }

        // Always remove from pinned folders if present, independent of whether it was in categories
        val currentPinned = getPinnedFolders().toMutableList()
        val removedFromPinned = currentPinned.removeAll { it.equals(trimmed, ignoreCase = true) }
        if (removedFromPinned) {
            savePinnedFoldersList(currentPinned)
        }

        val metadataMap = getMetadataMap()
        var modified = false
        val keys = metadataMap.keys()
        if (keys != null) {
            for (key in keys) {
                val docObj = metadataMap.optJSONObject(key) ?: continue
                var docModified = false

                val foldersArr = docObj.optJSONArray("folders")
                if (foldersArr != null) {
                    val updatedArr = JSONArray()
                    for (i in 0 until foldersArr.length()) {
                        val folderName = foldersArr.optString(i)
                        if (!folderName.equals(trimmed, ignoreCase = true)) {
                            updatedArr.put(folderName)
                        } else {
                            docModified = true
                        }
                    }
                    if (docModified) {
                        docObj.put("folders", updatedArr)
                    }
                }

                if (docObj.optString("category").equals(trimmed, ignoreCase = true)) {
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
        }
        if (modified) {
            saveMetadataMap(metadataMap)
        }

        removedFromCategories || removedFromPinned || modified
    }

    suspend fun getPinnedFolders(): List<String> = withContext(Dispatchers.IO) {
        val raw = prefs.getString(KEY_PINNED_FOLDERS, null)
        if (raw == null) {
            savePinnedFoldersList(DEFAULT_PINNED_FOLDERS)
            return@withContext DEFAULT_PINNED_FOLDERS
        }
        val items = deserializeStringList(raw)
        val list = mutableListOf<String>()
        for (item in items) {
            val trimmed = item.trim()
            if (trimmed.isNotEmpty() && !list.any { it.equals(trimmed, ignoreCase = true) }) {
                list.add(trimmed)
            }
        }
        list.take(MAX_PINNED_FOLDERS)
    }

    suspend fun setPinnedFolders(pinned: List<String>): Boolean = withContext(Dispatchers.IO) {
        val sanitized = pinned.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .take(MAX_PINNED_FOLDERS)
        savePinnedFoldersList(sanitized)
        true
    }

    suspend fun togglePinFolder(folder: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = folder.trim()
        if (trimmed.isEmpty()) return@withContext false
        val current = getPinnedFolders().toMutableList()
        val existingIndex = current.indexOfFirst { it.equals(trimmed, ignoreCase = true) }
        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
            savePinnedFoldersList(current)
            true
        } else {
            if (current.size >= MAX_PINNED_FOLDERS) {
                return@withContext false
            }
            current.add(trimmed)
            savePinnedFoldersList(current)
            true
        }
    }

    private fun savePinnedFoldersList(list: List<String>) {
        prefs.edit { putString(KEY_PINNED_FOLDERS, serializeStringList(list)) }
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
                    val folderName = foldersArr.optString(i).trim()
                    if (folderName.isNotEmpty() && !foldersList.contains(folderName)) {
                        foldersList.add(folderName)
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
                    val tagItem = tagsArr.optString(i).trim()
                    if (tagItem.isNotEmpty()) tagsList.add(tagItem)
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
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse document metadata map JSON", e)
            JSONObject()
        }
    }

    private fun saveMetadataMap(json: JSONObject) {
        prefs.edit { putString(KEY_METADATA_MAP, json.toString()) }
    }

    private fun saveCategoriesList(list: List<String>) {
        prefs.edit { putString(KEY_CATEGORIES, serializeStringList(list)) }
    }

    private fun serializeStringList(list: List<String>): String {
        return list.joinToString(separator = ",", prefix = "[", postfix = "]") { "\"${it.replace("\"", "\\\"")}\"" }
    }

    private fun deserializeStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val trimmed = raw.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return trimmed.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        }
        val content = trimmed.substring(1, trimmed.length - 1).trim()
        if (content.isEmpty()) return emptyList()
        val result = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()
        var i = 0
        while (i < content.length) {
            val c = content[i]
            if (c == '\\' && i + 1 < content.length) {
                sb.append(content[i + 1])
                i += 2
                continue
            }
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                val item = sb.toString().trim()
                if (item.isNotEmpty()) result.add(item)
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        val lastItem = sb.toString().trim()
        if (lastItem.isNotEmpty()) result.add(lastItem)
        return result
    }
}
