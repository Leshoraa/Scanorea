package com.leshoraa.scanorea.features.imagestopdf.ui

import android.net.Uri
import com.leshoraa.scanorea.core.filter.ImageFilterType
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ConversionProgress
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfConversionOptions
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfConversionResult
import com.leshoraa.scanorea.features.presets.domain.model.ConversionPreset
import com.leshoraa.scanorea.features.recentpdfs.domain.model.RecentPdf
import java.io.File

/**
 * Immutable presentation state for the Scanorea main screen and document pipeline.
 */
data class ImagesToPdfUiState(
    val pages: List<ImagePage> = emptyList(),
    val options: PdfConversionOptions = PdfConversionOptions(),
    val isConverting: Boolean = false,
    val conversionProgress: ConversionProgress? = null,
    val conversionResult: PdfConversionResult? = null,
    val errorMessage: String? = null,
    val isOptionsBottomSheetVisible: Boolean = false,
    val isGridView: Boolean = false,
    val defaultFilter: ImageFilterType = ImageFilterType.BLACK_AND_WHITE,
    val activeFilter: ImageFilterType = ImageFilterType.BLACK_AND_WHITE,
    val activePdfViewerFile: File? = null,
    val recentPdfs: List<RecentPdf> = emptyList(),
    val categories: List<String> = emptyList(),
    val pinnedFolders: List<String> = listOf("Favorites", "Work", "Personal"),
    val presets: List<ConversionPreset> = emptyList(),
    val destinationFolderUri: Uri? = null,
    val destinationFolderDisplayName: String = "Documents/Scanorea",
    val carouselPageIndex: Int = 0,
    val redoAnnotationsMap: Map<String, List<com.leshoraa.scanorea.features.editor.domain.model.PageAnnotation>> = emptyMap(),
    val undoPageHistoryMap: Map<String, List<ImagePage>> = emptyMap(),
    val redoPageHistoryMap: Map<String, List<ImagePage>> = emptyMap(),
    val selectedFolders: List<String> = emptyList(),
    val customTags: Map<String, String> = emptyMap()
) {
    val hasPages: Boolean
        get() = pages.isNotEmpty()

    val pageCount: Int
        get() = pages.size

    val currentPage: ImagePage?
        get() = pages.getOrNull(carouselPageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0)))

    fun canUndoAnnotation(pageId: String): Boolean =
        (undoPageHistoryMap[pageId]?.isNotEmpty() == true) ||
        (pages.find { it.id == pageId }?.annotations?.isNotEmpty() == true)

    fun canRedoAnnotation(pageId: String): Boolean =
        (redoPageHistoryMap[pageId]?.isNotEmpty() == true) ||
        (redoAnnotationsMap[pageId]?.isNotEmpty() == true)

    fun canUndo(pageId: String): Boolean =
        (undoPageHistoryMap[pageId]?.isNotEmpty() == true) || canUndoAnnotation(pageId)

    fun canRedo(pageId: String): Boolean =
        (redoPageHistoryMap[pageId]?.isNotEmpty() == true) || canRedoAnnotation(pageId)
}
