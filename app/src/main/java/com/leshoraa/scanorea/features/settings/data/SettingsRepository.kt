package com.leshoraa.scanorea.features.settings.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.leshoraa.scanorea.core.filter.ImageFilterType
import com.leshoraa.scanorea.features.imagestopdf.domain.model.CompressionProfile
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageOrientation
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageSize

/**
 * Persists user configuration and document defaults using [SharedPreferences].
 */
class SettingsRepository(context: Context) {

    companion object {
        private const val TAG = "SettingsRepository"
        private const val PREFS_NAME = "scanorea_settings_prefs"
        private const val KEY_DEFAULT_PAGE_SIZE = "key_default_page_size"
        private const val KEY_DEFAULT_ORIENTATION = "key_default_orientation"
        private const val KEY_DEFAULT_COMPRESSION = "key_default_compression"
        private const val KEY_DEFAULT_FILTER = "key_default_filter"
        private const val KEY_DEST_FOLDER_URI = "key_dest_folder_uri"
        private const val KEY_DEST_FOLDER_NAME = "key_dest_folder_name"
        private const val DEFAULT_DEST_NAME = "Internal: Scanorea/pdfs"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDefaultPageSize(): PdfPageSize {
        val raw = prefs.getString(KEY_DEFAULT_PAGE_SIZE, PdfPageSize.A4.name)
        return try {
            PdfPageSize.valueOf(raw ?: PdfPageSize.A4.name)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse default page size, defaulting to A4", e)
            PdfPageSize.A4
        }
    }

    fun saveDefaultPageSize(size: PdfPageSize) {
        prefs.edit { putString(KEY_DEFAULT_PAGE_SIZE, size.name) }
    }

    fun getDefaultOrientation(): PdfPageOrientation {
        val raw = prefs.getString(KEY_DEFAULT_ORIENTATION, PdfPageOrientation.PORTRAIT.name)
        return try {
            PdfPageOrientation.valueOf(raw ?: PdfPageOrientation.PORTRAIT.name)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse default orientation, defaulting to PORTRAIT", e)
            PdfPageOrientation.PORTRAIT
        }
    }

    fun saveDefaultOrientation(orientation: PdfPageOrientation) {
        prefs.edit { putString(KEY_DEFAULT_ORIENTATION, orientation.name) }
    }

    fun getDefaultCompression(): CompressionProfile {
        val raw = prefs.getString(KEY_DEFAULT_COMPRESSION, CompressionProfile.AUTO_BALANCED.name)
        return try {
            CompressionProfile.valueOf(raw ?: CompressionProfile.AUTO_BALANCED.name)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse default compression, defaulting to AUTO_BALANCED", e)
            CompressionProfile.AUTO_BALANCED
        }
    }

    fun saveDefaultCompression(profile: CompressionProfile) {
        prefs.edit { putString(KEY_DEFAULT_COMPRESSION, profile.name) }
    }

    fun getDefaultFilter(): ImageFilterType {
        val raw = prefs.getString(KEY_DEFAULT_FILTER, ImageFilterType.BLACK_AND_WHITE.name)
        return try {
            ImageFilterType.valueOf(raw ?: ImageFilterType.BLACK_AND_WHITE.name)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse default filter, defaulting to BLACK_AND_WHITE", e)
            ImageFilterType.BLACK_AND_WHITE
        }
    }

    fun saveDefaultFilter(filter: ImageFilterType) {
        prefs.edit { putString(KEY_DEFAULT_FILTER, filter.name) }
    }

    fun getDestinationFolderUri(): String? {
        return prefs.getString(KEY_DEST_FOLDER_URI, null)
    }

    fun getDestinationFolderDisplayName(): String {
        return prefs.getString(KEY_DEST_FOLDER_NAME, DEFAULT_DEST_NAME) ?: DEFAULT_DEST_NAME
    }

    fun saveDestinationFolder(uri: String?, displayName: String) {
        prefs.edit {
            putString(KEY_DEST_FOLDER_URI, uri)
            putString(KEY_DEST_FOLDER_NAME, displayName)
        }
    }
}
