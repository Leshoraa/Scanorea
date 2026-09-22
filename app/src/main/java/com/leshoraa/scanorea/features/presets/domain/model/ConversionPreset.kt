package com.leshoraa.scanorea.features.presets.domain.model

import com.leshoraa.scanorea.features.imagestopdf.domain.model.CompressionProfile
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageOrientation
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageSize

/**
 * Encapsulates a reusable configuration preset for document generation.
 *
 * @param id Unique identifier.
 * @param name User-visible preset name (e.g., "Aljabar").
 * @param fileNameTemplate Filename template supporting date tokens like `{DD:MM:YYYY}`.
 * @param pageSize Target paper dimension.
 * @param orientation Page orientation.
 * @param compressionProfile Compression and downsampling profile.
 */
data class ConversionPreset(
    val id: String,
    val name: String,
    val fileNameTemplate: String,
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val orientation: PdfPageOrientation = PdfPageOrientation.PORTRAIT,
    val compressionProfile: CompressionProfile = CompressionProfile.AUTO_BALANCED
)
