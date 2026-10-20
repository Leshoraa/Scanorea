package com.leshoraa.scanorea.app.ui

import android.net.Uri

/**
 * Actions that can be requested when the app is opened from outside its normal
 * launcher flow, such as an app shortcut or a shared document.
 */
enum class AppEntryAction {
    SCAN_DOCUMENT,
    IMPORT_PHOTOS,
    OPEN_PDF
}

/**
 * A single external entry request handed to [MainScreen].
 *
 * Image shares launch the editor with the shared pages. PDF shares and
 * "Open with" requests open the built-in viewer.
 */
data class AppEntryRequest(
    val action: AppEntryAction? = null,
    val imageUris: List<Uri> = emptyList(),
    val pdfUri: Uri? = null
)
