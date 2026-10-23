package com.leshoraa.scanorea.features.recentpdfs.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.leshoraa.scanorea.features.recentpdfs.data.db.CategoryEntity
import com.leshoraa.scanorea.features.recentpdfs.data.db.PinnedFolderEntity
import com.leshoraa.scanorea.features.recentpdfs.data.db.RecentPdfEntity
import com.leshoraa.scanorea.features.recentpdfs.data.db.ScanoreaDatabase
import com.leshoraa.scanorea.features.recentpdfs.domain.model.RecentPdf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Deep repository responsible for discovering, managing, and organizing
 * generated PDF documents with categories/folders, favorites, and tags.
 *
 * Backed by a high-performance indexed Room Database with fallback to
 * SharedPreferences for environment resilience.
 */
class RecentPdfsRepository(
    private val context: Context,
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    private val database: ScanoreaDatabase? = runCatching { ScanoreaDatabase.getInstance(context) }.getOrNull()
) {

    companion object {
        private const val TAG = "RecentPdfsRepository"
        private const val PREFS_NAME = "scanorea_document_metadata"
        private const val KEY_CATEGORIES = "document_categories"
        private const val KEY_PINNED_FOLDERS = "document_pinned_folders"
        private const val KEY_METADATA_MAP = "document_metadata_map"

        const val MAX_PINNED_FOLDERS = 5
        val DEFAULT_CATEGORIES = listOf("Work", "Study", "Personal", "Projects")
        val DEFAULT_PINNED_FOLDERS = listOf("Favorites", "Work", "Study", "Personal", "Projects")
    }

    private val dao = database?.recentPdfsDao()

    // In-memory caches for instant UI responsiveness and zero allocation spikes
    private var cachedCategories: MutableList<String>? = null
    private var cachedPinnedFolders: MutableList<String>? = null
    private var cachedMetadataMap: JSONObject? = null
    private val cachedPdfsByPath = ConcurrentHashMap<String, RecentPdf>()

    init {
        // Asynchronously check and migrate any legacy SharedPreferences data to Room
        if (dao != null) {
            CoroutineScope(Dispatchers.IO).launch {
                migrateLegacyPrefsToRoomIfNecessary()
            }
        }
    }

    private suspend fun migrateLegacyPrefsToRoomIfNecessary() {
        val currentDao = dao ?: return
        try {
            val existingCategories = currentDao.getCategories()
            if (existingCategories.isEmpty()) {
                val legacyCats = getLegacyCategories()
                val catsToSave = legacyCats.ifEmpty { DEFAULT_CATEGORIES }
                currentDao.upsertCategories(catsToSave.mapIndexed { idx, name -> CategoryEntity(name, idx) })
            }

            val existingPinned = currentDao.getPinnedFolders()
            if (existingPinned.isEmpty()) {
                val legacyPinned = getLegacyPinnedFolders()
                val pinnedToSave = legacyPinned.ifEmpty { DEFAULT_PINNED_FOLDERS }
                currentDao.upsertPinnedFolders(pinnedToSave.mapIndexed { idx, name -> PinnedFolderEntity(name, idx) })
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Legacy migration check caught exception", e)
        }
    }

    suspend fun getCategories(): List<String> = withContext(Dispatchers.IO) {
        cachedCategories?.let { return@withContext it.toList() }

        val currentDao = dao
        if (currentDao != null) {
            val fromDb = try { currentDao.getCategories() } catch (_: Throwable) { null }
            if (!fromDb.isNullOrEmpty()) {
                val list = fromDb.map { it.name }.toMutableList()
                cachedCategories = list
                return@withContext list.toList()
            }
        }

        val list = getLegacyCategories().toMutableList()
        if (list.isEmpty()) {
            val defaults = DEFAULT_CATEGORIES.toMutableList()
            cachedCategories = defaults
            saveCategoriesList(defaults)
            return@withContext defaults.toList()
        }
        cachedCategories = list
        list.toList()
    }

    private fun getLegacyCategories(): List<String> {
        val raw = prefs.getString(KEY_CATEGORIES, null) ?: return emptyList()
        val items = deserializeStringList(raw)
        val list = mutableListOf<String>()
        for (item in items) {
            val trimmed = item.trim()
            if (trimmed.isNotEmpty() && !trimmed.equals("Receipts", ignoreCase = true) && !list.contains(trimmed)) {
                list.add(trimmed)
            }
        }
        return list
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
        cachedCategories = current
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
        cachedCategories = current
        saveCategoriesList(current)

        // Update in Room Database if available
        val currentDao = dao
        if (currentDao != null) {
            try {
                val allEntities = currentDao.getAllPdfs()
                allEntities.forEach { entity ->
                    val folders = deserializeStringList(entity.foldersJson)
                    if (folders.any { it.equals(oldTrimmed, ignoreCase = true) }) {
                        val updated = folders.map { if (it.equals(oldTrimmed, ignoreCase = true)) newTrimmed else it }
                        currentDao.upsertPdf(entity.copy(foldersJson = serializeStringList(updated)))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error renaming category in Room", e)
            }
        }

        // Update all documents assigned to oldCategory in metadata prefs map
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
            cachedPdfsByPath.clear()
        }

        // Keep pinned folders list in sync with renamed category
        val currentPinned = getPinnedFolders().toMutableList()
        val pinnedIndex = currentPinned.indexOfFirst { it.equals(oldTrimmed, ignoreCase = true) }
        if (pinnedIndex >= 0) {
            currentPinned[pinnedIndex] = newTrimmed
            cachedPinnedFolders = currentPinned
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
            cachedCategories = current
            saveCategoriesList(current)
        }

        // Always remove from pinned folders if present, independent of whether it was in categories
        val currentPinned = getPinnedFolders().toMutableList()
        val removedFromPinned = currentPinned.removeAll { it.equals(trimmed, ignoreCase = true) }
        if (removedFromPinned) {
            cachedPinnedFolders = currentPinned
            savePinnedFoldersList(currentPinned)
        }

        // Update in Room Database if available
        val currentDao = dao
        if (currentDao != null) {
            try {
                currentDao.deleteCategory(trimmed)
                val allEntities = currentDao.getAllPdfs()
                allEntities.forEach { entity ->
                    val folders = deserializeStringList(entity.foldersJson)
                    if (folders.any { it.equals(trimmed, ignoreCase = true) }) {
                        val updated = folders.filterNot { it.equals(trimmed, ignoreCase = true) }
                        currentDao.upsertPdf(entity.copy(foldersJson = serializeStringList(updated)))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting category in Room", e)
            }
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
            cachedPdfsByPath.clear()
        }

        removedFromCategories || removedFromPinned || modified
    }

    suspend fun getPinnedFolders(): List<String> = withContext(Dispatchers.IO) {
        cachedPinnedFolders?.let { return@withContext it.toList() }

        val currentDao = dao
        if (currentDao != null) {
            val fromDb = try { currentDao.getPinnedFolders() } catch (_: Throwable) { null }
            if (!fromDb.isNullOrEmpty()) {
                val list = fromDb.map { it.name }.take(MAX_PINNED_FOLDERS).toMutableList()
                cachedPinnedFolders = list
                return@withContext list.toList()
            }
        }

        val list = getLegacyPinnedFolders().toMutableList()
        if (list.isEmpty()) {
            val defaults = DEFAULT_PINNED_FOLDERS.toMutableList()
            cachedPinnedFolders = defaults
            savePinnedFoldersList(defaults)
            return@withContext defaults.toList()
        }
        val result = list.take(MAX_PINNED_FOLDERS).toMutableList()
        cachedPinnedFolders = result
        result.toList()
    }

    private fun getLegacyPinnedFolders(): List<String> {
        val raw = prefs.getString(KEY_PINNED_FOLDERS, null) ?: return emptyList()
        val items = deserializeStringList(raw)
        val list = mutableListOf<String>()
        for (item in items) {
            val trimmed = item.trim()
            if (trimmed.isNotEmpty() && !list.any { it.equals(trimmed, ignoreCase = true) }) {
                list.add(trimmed)
            }
        }
        return list
    }

    suspend fun setPinnedFolders(pinned: List<String>): Boolean = withContext(Dispatchers.IO) {
        val sanitized = pinned.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .take(MAX_PINNED_FOLDERS)
            .toMutableList()
        cachedPinnedFolders = sanitized
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
            cachedPinnedFolders = current
            savePinnedFoldersList(current)
            true
        } else {
            if (current.size >= MAX_PINNED_FOLDERS) {
                return@withContext false
            }
            current.add(trimmed)
            cachedPinnedFolders = current
            savePinnedFoldersList(current)
            true
        }
    }

    private fun savePinnedFoldersList(list: List<String>) {
        prefs.edit { putString(KEY_PINNED_FOLDERS, serializeStringList(list)) }
        val currentDao = dao
        if (currentDao != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    currentDao.clearPinnedFolders()
                    currentDao.upsertPinnedFolders(list.mapIndexed { idx, name -> PinnedFolderEntity(name, idx) })
                } catch (e: Exception) {
                    Log.w(TAG, "Error saving pinned folders in Room", e)
                }
            }
        }
    }

    suspend fun getRecentPdfs(): List<RecentPdf> = withContext(Dispatchers.IO) {
        val pdfDir = File(context.filesDir, "pdfs")
        if (!pdfDir.exists() || !pdfDir.isDirectory) {
            cachedPdfsByPath.clear()
            return@withContext emptyList()
        }

        val metadataMap = getMetadataMap()
        val pdfFiles = pdfDir.listFiles { file ->
            file.isFile && file.name.endsWith(".pdf", ignoreCase = true)
        } ?: return@withContext emptyList()

        val existingFilePaths = HashSet<String>(pdfFiles.size)
        val currentDao = dao
        val dbEntitiesMap = try {
            currentDao?.getAllPdfs()?.associateBy { it.filePath } ?: emptyMap()
        } catch (_: Throwable) {
            emptyMap()
        }

        val entitiesToUpsert = mutableListOf<RecentPdfEntity>()

        val pdfList = pdfFiles.map { file ->
            val path = file.absolutePath
            existingFilePaths.add(path)
            val lastModified = file.lastModified()
            val sizeBytes = file.length()

            val existing = cachedPdfsByPath[path]
            val dbEntity = dbEntitiesMap[path]
            val docObj = metadataMap.optJSONObject(file.name)

            val isFavorite = dbEntity?.isFavorite ?: (docObj?.optBoolean("isFavorite", false) ?: false)
            val foldersList = mutableListOf<String>()

            if (dbEntity != null) {
                foldersList.addAll(deserializeStringList(dbEntity.foldersJson))
            } else if (docObj?.has("folders") == true) {
                val foldersArr = docObj.optJSONArray("folders")
                if (foldersArr != null) {
                    for (i in 0 until foldersArr.length()) {
                        val folderName = foldersArr.optString(i).trim()
                        if (folderName.isNotEmpty() && !foldersList.contains(folderName)) {
                            foldersList.add(folderName)
                        }
                    }
                }
            } else {
                val legacyCategory = docObj?.optString("category")?.trim()
                if (!legacyCategory.isNullOrEmpty()) {
                    foldersList.add(legacyCategory)
                }
            }

            val tagsList = mutableListOf<String>()
            if (dbEntity != null) {
                tagsList.addAll(deserializeStringList(dbEntity.tagsJson))
            } else {
                val tagsArr = docObj?.optJSONArray("tags")
                if (tagsArr != null) {
                    for (i in 0 until tagsArr.length()) {
                        val tagItem = tagsArr.optString(i).trim()
                        if (tagItem.isNotEmpty()) tagsList.add(tagItem)
                    }
                }
            }

            // Collect entities needing sync into batch list for atomic Room DB upsert
            if (currentDao != null && (dbEntity == null || dbEntity.lastModified != lastModified || dbEntity.fileSizeBytes != sizeBytes)) {
                entitiesToUpsert.add(
                    RecentPdfEntity(
                        filePath = path,
                        fileName = file.name,
                        fileSizeBytes = sizeBytes,
                        lastModified = lastModified,
                        pageCount = dbEntity?.pageCount ?: 1,
                        isFavorite = isFavorite,
                        foldersJson = serializeStringList(foldersList),
                        tagsJson = serializeStringList(tagsList),
                        customTitle = dbEntity?.customTitle
                    )
                )
            }

            if (existing != null &&
                existing.lastModifiedMillis == lastModified &&
                existing.sizeBytes == sizeBytes &&
                existing.isFavorite == isFavorite &&
                existing.folders == foldersList &&
                existing.tags == tagsList
            ) {
                existing
            } else {
                val newPdf = RecentPdf(
                    file = file,
                    name = file.name,
                    sizeBytes = sizeBytes,
                    lastModifiedMillis = lastModified,
                    folders = foldersList,
                    isFavorite = isFavorite,
                    tags = tagsList
                )
                cachedPdfsByPath[path] = newPdf
                newPdf
            }
        }.sortedByDescending { it.lastModifiedMillis }

        // Batch upsert unsynced or modified entities atomically
        if (currentDao != null && entitiesToUpsert.isNotEmpty()) {
            try {
                currentDao.upsertPdfs(entitiesToUpsert)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to batch upsert PDFs to Room", e)
            }
        }

        // Clean up metadata keys and Room records for removed files
        if (currentDao != null) {
            currentDao.deletePdfsNotIn(existingFilePaths.toList())
        }

        val keys = metadataMap.keys()
        val keysToRemove = mutableListOf<String>()
        if (keys != null) {
            for (key in keys) {
                if (pdfFiles.none { it.name == key }) {
                    keysToRemove.add(key)
                }
            }
        }
        if (keysToRemove.isNotEmpty()) {
            keysToRemove.forEach { key ->
                metadataMap.remove(key)
                val matchingPaths = cachedPdfsByPath.keys.filter { File(it).name == key }
                matchingPaths.forEach { cachedPdfsByPath.remove(it) }
            }
            saveMetadataMap(metadataMap)
        }

        pdfList
    }

    suspend fun updatePdfFolders(file: File, folders: List<String>): Boolean = withContext(Dispatchers.IO) {
        val distinctFolders = folders.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val foldersJson = serializeStringList(distinctFolders)

        val currentDao = dao
        if (currentDao != null) {
            currentDao.updateFolders(file.absolutePath, foldersJson)
        }

        val metadataMap = getMetadataMap()
        val docObj = metadataMap.optJSONObject(file.name) ?: JSONObject()
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

        cachedPdfsByPath.remove(file.absolutePath)
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

        val currentDao = dao
        if (currentDao != null) {
            currentDao.updateFavorite(file.absolutePath, newFav)
        }

        cachedPdfsByPath.remove(file.absolutePath)
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

        val currentDao = dao
        if (currentDao != null) {
            val existing = currentDao.getPdfByPath(file.absolutePath)
            if (existing != null) {
                currentDao.upsertPdf(existing.copy(tagsJson = serializeStringList(tags)))
            }
        }

        cachedPdfsByPath.remove(file.absolutePath)
        true
    }

    suspend fun deletePdf(file: File): Boolean = withContext(Dispatchers.IO) {
        val currentDao = dao
        if (currentDao != null) {
            currentDao.deletePdfByPath(file.absolutePath)
        }

        val metadataMap = getMetadataMap()
        if (metadataMap.has(file.name)) {
            metadataMap.remove(file.name)
            saveMetadataMap(metadataMap)
        }
        cachedPdfsByPath.remove(file.absolutePath)

        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    private fun getMetadataMap(): JSONObject {
        cachedMetadataMap?.let { return it }
        val raw = prefs.getString(KEY_METADATA_MAP, null)
        val json = if (raw.isNullOrBlank()) {
            JSONObject()
        } else {
            try {
                JSONObject(raw)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse document metadata map JSON", e)
                JSONObject()
            }
        }
        cachedMetadataMap = json
        return json
    }

    private fun saveMetadataMap(json: JSONObject) {
        cachedMetadataMap = json
        prefs.edit { putString(KEY_METADATA_MAP, json.toString()) }
    }

    private fun saveCategoriesList(list: List<String>) {
        cachedCategories = list.toMutableList()
        prefs.edit { putString(KEY_CATEGORIES, serializeStringList(list)) }
        val currentDao = dao
        if (currentDao != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    currentDao.clearCategories()
                    currentDao.upsertCategories(list.mapIndexed { idx, name -> CategoryEntity(name, idx) })
                } catch (e: Exception) {
                    Log.w(TAG, "Error saving categories in Room", e)
                }
            }
        }
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
