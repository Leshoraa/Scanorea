package com.leshoraa.scanorea.features.settings.data

import android.content.Context
import android.content.SharedPreferences
import com.leshoraa.scanorea.core.filter.ImageFilterType
import com.leshoraa.scanorea.features.imagestopdf.domain.model.CompressionProfile
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageOrientation
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageSize

/**
 * Persists user configuration and document defaults using [SharedPreferences].
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDefaultPageSize(): PdfPageSize {
        val raw = prefs.getString(KEY_DEFAULT_PAGE_SIZE, PdfPageSize.A4.name)
        return try {
            PdfPageSize.valueOf(raw ?: PdfPageSize.A4.name)
        } catch (_: Exception) {
            PdfPageSize.A4
        }
    }

    fun saveDefaultPageSize(size: PdfPageSize) {
        prefs.edit().putString(KEY_DEFAULT_PAGE_SIZE, size.name).apply()
    }

    fun getDefaultOrientation(): PdfPageOrientation {
        val raw = prefs.getString(KEY_DEFAULT_ORIENTATION, PdfPageOrientation.PORTRAIT.name)
        return try {
            PdfPageOrientation.valueOf(raw ?: PdfPageOrientation.PORTRAIT.name)
        } catch (_: Exception) {
            PdfPageOrientation.PORTRAIT
        }
    }

    fun saveDefaultOrientation(orientation: PdfPageOrientation) {
        prefs.edit().putString(KEY_DEFAULT_ORIENTATION, orientation.name).apply()
    }

    fun getDefaultCompression(): CompressionProfile {
        val raw = prefs.getString(KEY_DEFAULT_COMPRESSION, CompressionProfile.AUTO_BALANCED.name)
        return try {
            CompressionProfile.valueOf(raw ?: CompressionProfile.AUTO_BALANCED.name)
        } catch (_: Exception) {
            CompressionProfile.AUTO_BALANCED
        }
    }

    fun saveDefaultCompression(profile: CompressionProfile) {
        prefs.edit().putString(KEY_DEFAULT_COMPRESSION, profile.name).apply()
    }

    fun getDefaultFilter(): ImageFilterType {
        val raw = prefs.getString(KEY_DEFAULT_FILTER, ImageFilterType.BLACK_AND_WHITE.name)
        return try {
            ImageFilterType.valueOf(raw ?: ImageFilterType.BLACK_AND_WHITE.name)
        } catch (_: Exception) {
            ImageFilterType.BLACK_AND_WHITE
        }
    }

    fun saveDefaultFilter(filter: ImageFilterType) {
        prefs.edit().putString(KEY_DEFAULT_FILTER, filter.name).apply()
    }

    fun getDestinationFolderUri(): String? {
        return prefs.getString(KEY_DEST_FOLDER_URI, null)
    }

    fun getDestinationFolderDisplayName(): String {
        return prefs.getString(KEY_DEST_FOLDER_NAME, DEFAULT_DEST_NAME) ?: DEFAULT_DEST_NAME
    }

    fun saveDestinationFolder(uri: String?, displayName: String) {
        prefs.edit()
            .putString(KEY_DEST_FOLDER_URI, uri)
            .putString(KEY_DEST_FOLDER_NAME, displayName)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "scanorea_settings_prefs"
        private const val KEY_DEFAULT_PAGE_SIZE = "key_default_page_size"
        private const val KEY_DEFAULT_ORIENTATION = "key_default_orientation"
        private const val KEY_DEFAULT_COMPRESSION = "key_default_compression"
        private const val KEY_DEFAULT_FILTER = "key_default_filter"
        private const val KEY_DEST_FOLDER_URI = "key_dest_folder_uri"
        private const val KEY_DEST_FOLDER_NAME = "key_dest_folder_name"
        private const val DEFAULT_DEST_NAME = "Internal: Scanorea/pdfs"
    }
}
