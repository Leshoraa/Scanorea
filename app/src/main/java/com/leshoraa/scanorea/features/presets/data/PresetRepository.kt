package com.leshoraa.scanorea.features.presets.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.leshoraa.scanorea.features.imagestopdf.domain.model.CompressionProfile
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageOrientation
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageSize
import com.leshoraa.scanorea.features.presets.domain.model.ConversionPreset
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages persistent storage and retrieval of conversion presets using [SharedPreferences].
 */
class PresetRepository(context: Context) {

    companion object {
        private const val TAG = "PresetRepository"
        private const val PREFS_NAME = "scanorea_presets"
        private const val KEY_PRESETS = "saved_conversion_presets"
        private val DUMMY_PRESET_IDS = setOf("preset_tugas", "preset_catatan", "preset_aljabar")
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getPresets(): List<ConversionPreset> {
        val rawJson = prefs.getString(KEY_PRESETS, null)
        if (rawJson.isNullOrBlank()) {
            return emptyList()
        }

        return try {
            val jsonArray = JSONArray(rawJson)
            val list = mutableListOf<ConversionPreset>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                // Exclude legacy dummy presets
                if (id in DUMMY_PRESET_IDS) continue

                list.add(
                    ConversionPreset(
                        id = id,
                        name = obj.getString("name"),
                        fileNameTemplate = obj.getString("fileNameTemplate"),
                        pageSize = try {
                            PdfPageSize.valueOf(obj.getString("pageSize"))
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse pageSize for preset, falling back to A4", e)
                            PdfPageSize.A4
                        },
                        orientation = try {
                            PdfPageOrientation.valueOf(obj.getString("orientation"))
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse orientation for preset, falling back to PORTRAIT", e)
                            PdfPageOrientation.PORTRAIT
                        },
                        compressionProfile = try {
                            CompressionProfile.valueOf(obj.getString("compressionProfile"))
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse compressionProfile for preset, falling back to AUTO_BALANCED", e)
                            CompressionProfile.AUTO_BALANCED
                        }
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse presets JSON", e)
            emptyList()
        }
    }

    fun savePreset(preset: ConversionPreset) {
        val current = getPresets().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == preset.id || it.name.equals(preset.name, ignoreCase = true) }
        if (existingIndex >= 0) {
            current[existingIndex] = preset
        } else {
            current.add(preset)
        }
        savePresetsList(current)
    }

    fun deletePreset(presetId: String) {
        val updated = getPresets().filterNot { it.id == presetId }
        savePresetsList(updated)
    }

    private fun savePresetsList(list: List<ConversionPreset>) {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("fileNameTemplate", item.fileNameTemplate)
                put("pageSize", item.pageSize.name)
                put("orientation", item.orientation.name)
                put("compressionProfile", item.compressionProfile.name)
            }
            jsonArray.put(obj)
        }
        prefs.edit { putString(KEY_PRESETS, jsonArray.toString()) }
    }
}
