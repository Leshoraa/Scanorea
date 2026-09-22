package com.leshoraa.scanorea.features.imagestopdf.domain.model

import android.net.Uri
import java.io.File

/**
 * Result data payload when PDF creation finishes successfully.
 */
data class PdfConversionResult(
    val file: File,
    val contentUri: Uri,
    val pageCount: Int,
    val fileSizeBytes: Long
)
