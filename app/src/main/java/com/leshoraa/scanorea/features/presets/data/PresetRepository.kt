package com.leshoraa.scanorea.features.presets.data

import android.content.Context
import android.content.SharedPreferences
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

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getPresets(): List<ConversionPreset> {
        val rawJson = prefs.getString(KEY_PRESETS, null)
        if (rawJson.isNullOrBlank()) {
            val defaults = listOf(defaultAljabarPreset())
            savePresetsList(defaults)
            return defaults
        }

        return try {
            val jsonArray = JSONArray(rawJson)
            val list = mutableListOf<ConversionPreset>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ConversionPreset(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        fileNameTemplate = obj.getString("fileNameTemplate"),
                        pageSize = try {
                            PdfPageSize.valueOf(obj.getString("pageSize"))
                        } catch (_: Exception) {
                            PdfPageSize.A4
                        },
                        orientation = try {
                            PdfPageOrientation.valueOf(obj.getString("orientation"))
                        } catch (_: Exception) {
                            PdfPageOrientation.PORTRAIT
                        },
                        compressionProfile = try {
                            CompressionProfile.valueOf(obj.getString("compressionProfile"))
                        } catch (_: Exception) {
                            CompressionProfile.AUTO_BALANCED
                        }
                    )
                )
            }
            if (list.isEmpty()) {
                val defaults = listOf(defaultAljabarPreset())
                savePresetsList(defaults)
                defaults
            } else {
                list
            }
        } catch (_: Exception) {
            listOf(defaultAljabarPreset())
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
        prefs.edit().putString(KEY_PRESETS, jsonArray.toString()).apply()
    }

    private fun defaultAljabarPreset(): ConversionPreset {
        return ConversionPreset(
            id = "preset_aljabar",
            name = "Aljabar",
            fileNameTemplate = "Rendra_23.XX.XXXX_Aljabar_{DD:MM:YYYY}",
            pageSize = PdfPageSize.A4,
            orientation = PdfPageOrientation.PORTRAIT,
            compressionProfile = CompressionProfile.AUTO_BALANCED
        )
    }

    companion object {
        private const val PREFS_NAME = "scanorea_presets"
        private const val KEY_PRESETS = "saved_conversion_presets"
    }
}
