package com.leshoraa.scanorea.features.imagestopdf.ui

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leshoraa.scanorea.core.common.ResourceResult
import com.leshoraa.scanorea.core.filter.ImageAnalyzer
import com.leshoraa.scanorea.core.filter.ImageFilterType
import com.leshoraa.scanorea.features.imagestopdf.data.ImageDecoderDataSource
import com.leshoraa.scanorea.features.imagestopdf.data.PdfGeneratorDataSource
import com.leshoraa.scanorea.features.imagestopdf.data.repository.PdfConversionRepositoryImpl
import com.leshoraa.scanorea.features.imagestopdf.domain.model.CompressionProfile
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ConversionProgress
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImageCropBounds
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage
import com.leshoraa.scanorea.features.editor.domain.model.PageAnnotation
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfConversionOptions
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageOrientation
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageSize
import com.leshoraa.scanorea.features.imagestopdf.domain.repository.PdfConversionRepository
import com.leshoraa.scanorea.features.presets.data.PresetRepository
import com.leshoraa.scanorea.features.presets.domain.TemplateDateEvaluator
import com.leshoraa.scanorea.features.presets.domain.model.ConversionPreset
import com.leshoraa.scanorea.features.recentpdfs.data.PdfThumbnailLoader
import com.leshoraa.scanorea.features.recentpdfs.data.RecentPdfsRepository
import com.leshoraa.scanorea.features.settings.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * ViewModel managing UI interactions, image collection state, per-image filters and adjustments,
 * preset management with dynamic date tokens, and triggering PDF conversion.
 */
class ImagesToPdfViewModel(
    private val pdfConversionRepository: PdfConversionRepository,
    private val recentPdfsRepository: RecentPdfsRepository? = null,
    private val presetRepository: PresetRepository? = null,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImagesToPdfUiState())
    val uiState: StateFlow<ImagesToPdfUiState> = _uiState.asStateFlow()

    init {
        loadRecentPdfs()
        loadPresets()
        loadSettings()
    }

    fun loadSettings() {
        if (settingsRepository == null) return
        val pageSize = settingsRepository.getDefaultPageSize()
        val orientation = settingsRepository.getDefaultOrientation()
        val compression = settingsRepository.getDefaultCompression()
        val defaultFilter = settingsRepository.getDefaultFilter()
        val destUri = settingsRepository.getDestinationFolderUri()
        val destName = settingsRepository.getDestinationFolderDisplayName()

        _uiState.update { current ->
            current.copy(
                destinationFolderUri = destUri?.let { Uri.parse(it) },
                destinationFolderDisplayName = destName,
                defaultFilter = defaultFilter,
                activeFilter = defaultFilter,
                options = current.options.copy(
                    pageSize = pageSize,
                    orientation = orientation,
                    compressionProfile = compression
                )
            )
        }
    }

    fun loadRecentPdfs() {
        if (recentPdfsRepository == null) return
        viewModelScope.launch {
            val recents = recentPdfsRepository.getRecentPdfs()
            val categories = recentPdfsRepository.getCategories()
            _uiState.update { it.copy(recentPdfs = recents, categories = categories) }
        }
    }

    fun loadPresets() {
        if (presetRepository == null) return
        viewModelScope.launch {
            val presets = presetRepository.getPresets()
            _uiState.update { it.copy(presets = presets) }
        }
    }

    fun applyPreset(preset: ConversionPreset) {
        val evaluatedName = TemplateDateEvaluator.evaluate(preset.fileNameTemplate)
        _uiState.update { currentState ->
            currentState.copy(
                options = currentState.options.copy(
                    fileName = evaluatedName,
                    pageSize = preset.pageSize,
                    orientation = preset.orientation,
                    compressionProfile = preset.compressionProfile
                )
            )
        }
    }

    fun savePreset(preset: ConversionPreset) {
        presetRepository?.savePreset(preset)
        loadPresets()
    }

    fun saveNewPreset(name: String, template: String) {
        val options = _uiState.value.options
        val newPreset = ConversionPreset(
            id = "preset_${System.currentTimeMillis()}",
            name = name,
            fileNameTemplate = template,
            pageSize = options.pageSize,
            orientation = options.orientation,
            compressionProfile = options.compressionProfile
        )
        presetRepository?.savePreset(newPreset)
        loadPresets()
    }

    fun deletePreset(presetId: String) {
        presetRepository?.deletePreset(presetId)
        loadPresets()
    }

    fun updateDefaultPageSize(size: PdfPageSize) {
        settingsRepository?.saveDefaultPageSize(size)
        _uiState.update { it.copy(options = it.options.copy(pageSize = size)) }
    }

    fun updateDefaultOrientation(orientation: PdfPageOrientation) {
        settingsRepository?.saveDefaultOrientation(orientation)
        _uiState.update { it.copy(options = it.options.copy(orientation = orientation)) }
    }

    fun updateDefaultCompression(compression: CompressionProfile) {
        settingsRepository?.saveDefaultCompression(compression)
        _uiState.update { it.copy(options = it.options.copy(compressionProfile = compression)) }
    }

    fun updateDefaultFilter(filter: ImageFilterType) {
        settingsRepository?.saveDefaultFilter(filter)
        _uiState.update { it.copy(defaultFilter = filter, activeFilter = filter) }
    }

    fun setCustomDestinationFolder(uri: Uri?, displayName: String) {
        settingsRepository?.saveDestinationFolder(uri?.toString(), displayName)
        _uiState.update {
            it.copy(
                destinationFolderUri = uri,
                destinationFolderDisplayName = displayName
            )
        }
    }

    fun openPdfInViewer(file: File) {
        _uiState.update { it.copy(activePdfViewerFile = file) }
    }

    fun closePdfViewer() {
        _uiState.update { it.copy(activePdfViewerFile = null) }
    }

    fun deleteRecentPdf(file: File) {
        if (recentPdfsRepository == null) return
        PdfThumbnailLoader.evict(file)
        viewModelScope.launch {
            recentPdfsRepository.deletePdf(file)
            loadRecentPdfs()
        }
    }

    fun toggleFavorite(file: File) {
        if (recentPdfsRepository == null) return
        viewModelScope.launch {
            recentPdfsRepository.toggleFavorite(file)
            loadRecentPdfs()
        }
    }

    fun updatePdfCategory(file: File, category: String?) {
        if (recentPdfsRepository == null) return
        viewModelScope.launch {
            recentPdfsRepository.updatePdfCategory(file, category)
            loadRecentPdfs()
        }
    }

    fun addCategory(category: String) {
        if (recentPdfsRepository == null) return
        viewModelScope.launch {
            recentPdfsRepository.addCategory(category)
            loadRecentPdfs()
        }
    }

    fun deleteCategory(category: String) {
        if (recentPdfsRepository == null) return
        viewModelScope.launch {
            recentPdfsRepository.deleteCategory(category)
            loadRecentPdfs()
        }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun applyFilterToAll(filter: ImageFilterType) {
        _uiState.update { currentState ->
            currentState.copy(
                activeFilter = filter,
                pages = currentState.pages.map { it.copy(filter = filter) }
            )
        }
    }

    fun updatePageFilter(pageId: String, filter: ImageFilterType) {
        _uiState.update { currentState ->
            val updatedPages = currentState.pages.map { page ->
                if (page.id == pageId) page.copy(filter = filter) else page
            }
            currentState.copy(pages = updatedPages, activeFilter = filter)
        }
    }

    fun updatePageAdjustment(pageId: String, contrast: Float, brightness: Float) {
        _uiState.update { currentState ->
            val updatedPages = currentState.pages.map { page ->
                if (page.id == pageId) page.copy(contrast = contrast, brightness = brightness) else page
            }
            currentState.copy(pages = updatedPages)
        }
    }

    fun resetPageAdjustment(pageId: String) {
        _uiState.update { currentState ->
            val updatedPages = currentState.pages.map { page ->
                if (page.id == pageId) {
                    page.copy(contrast = page.initialContrast, brightness = page.initialBrightness)
                } else {
                    page
                }
            }
            currentState.copy(pages = updatedPages)
        }
    }

    fun cycleFilterForPage(pageId: String) {
        _uiState.update { currentState ->
            val updatedPages = currentState.pages.map { page ->
                if (page.id == pageId) {
                    val nextFilter = when (page.filter) {
                        ImageFilterType.ORIGINAL -> ImageFilterType.BLACK_AND_WHITE
                        ImageFilterType.BLACK_AND_WHITE -> ImageFilterType.GRAYSCALE
                        ImageFilterType.GRAYSCALE -> ImageFilterType.ENHANCED
                        ImageFilterType.ENHANCED -> ImageFilterType.ORIGINAL
                    }
                    page.copy(filter = nextFilter)
                } else {
                    page
                }
            }
            currentState.copy(pages = updatedPages)
        }
    }

    fun addImages(uris: List<Uri>, contentResolver: ContentResolver) {
        if (uris.isEmpty()) return

        val defaultFilter = _uiState.value.defaultFilter
        val newPages = uris.map { uri ->
            val metadata = queryUriMetadata(uri, contentResolver)
            ImagePage(
                id = UUID.randomUUID().toString(),
                uri = uri,
                displayName = metadata.displayName,
                sizeInBytes = metadata.sizeInBytes,
                width = metadata.width,
                height = metadata.height,
                filter = defaultFilter,
                contrast = 1.0f,
                brightness = 0.0f
            )
        }

        _uiState.update { currentState ->
            currentState.copy(pages = currentState.pages + newPages)
        }
    }

    /**
     * Auto-adjusts the sharpness/contrast and brightness for a specific page using dynamic image analysis
     * and updates the baseline recommendation.
     */
    fun autoAdjustPage(pageId: String, contentResolver: ContentResolver) {
        val page = _uiState.value.pages.find { it.id == pageId } ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val analysis = ImageAnalyzer.analyze(page.uri, contentResolver)
            recordAutoCalibration(pageId, analysis.optimalContrast, analysis.optimalBrightness)
        }
    }

    /**
     * Persists the device's auto-recommended contrast and brightness as the baseline recommendation.
     */
    fun recordAutoCalibration(pageId: String, optimalContrast: Float, optimalBrightness: Float) {
        _uiState.update { currentState ->
            val updatedPages = currentState.pages.map { page ->
                if (page.id == pageId) {
                    page.copy(
                        contrast = optimalContrast,
                        brightness = optimalBrightness,
                        initialContrast = optimalContrast,
                        initialBrightness = optimalBrightness
                    )
                } else {
                    page
                }
            }
            currentState.copy(pages = updatedPages)
        }
    }

    fun removePage(pageId: String) {
        _uiState.update { currentState ->
            currentState.copy(pages = currentState.pages.filterNot { it.id == pageId })
        }
    }

    fun movePage(sourceIndex: Int, targetIndex: Int) {
        val currentPages = _uiState.value.pages
        if (sourceIndex == targetIndex || sourceIndex !in currentPages.indices || targetIndex !in currentPages.indices) return
        _uiState.update { currentState ->
            val mutableList = currentState.pages.toMutableList()
            val item = mutableList.removeAt(sourceIndex)
            mutableList.add(targetIndex, item)
            currentState.copy(pages = mutableList)
        }
    }

    fun movePageUp(index: Int) {
        movePage(index, index - 1)
    }

    fun movePageDown(index: Int) {
        movePage(index, index + 1)
    }

    fun clearAllPages() {
        _uiState.update { currentState ->
            currentState.copy(pages = emptyList())
        }
    }

    fun rotatePage(pageId: String) {
        _uiState.update { currentState ->
            val updatedPages = currentState.pages.map { page ->
                if (page.id == pageId) {
                    val nextRotation = (page.rotationDegrees + 90) % 360
                    page.copy(rotationDegrees = nextRotation)
                } else {
                    page
                }
            }
            currentState.copy(pages = updatedPages)
        }
    }

    fun rotateAllPages() {
        _uiState.update { currentState ->
            val updatedPages = currentState.pages.map { page ->
                val nextRotation = (page.rotationDegrees + 90) % 360
                page.copy(rotationDegrees = nextRotation)
            }
            currentState.copy(pages = updatedPages)
        }
    }

    fun updatePageCrop(pageId: String, cropBounds: ImageCropBounds) {
        _uiState.update { currentState ->
            val updatedPages = currentState.pages.map { page ->
                if (page.id == pageId) page.copy(cropBounds = cropBounds) else page
            }
            currentState.copy(pages = updatedPages)
        }
    }

    fun resetPageCrop(pageId: String) {
        updatePageCrop(pageId, ImageCropBounds.DEFAULT)
    }

    fun addPageAnnotation(pageId: String, annotation: PageAnnotation) {
        _uiState.update { currentState ->
            val updatedPages = currentState.pages.map { page ->
                if (page.id == pageId) {
                    page.copy(annotations = page.annotations + annotation)
                } else {
                    page
                }
            }
            val updatedRedo = currentState.redoAnnotationsMap + (pageId to emptyList())
            currentState.copy(pages = updatedPages, redoAnnotationsMap = updatedRedo)
        }
    }

    fun undoPageAnnotation(pageId: String) {
        _uiState.update { currentState ->
            val targetPage = currentState.pages.find { it.id == pageId } ?: return@update currentState
            if (targetPage.annotations.isEmpty()) return@update currentState

            val annotationToUndo = targetPage.annotations.last()
            val updatedPages = currentState.pages.map { page ->
                if (page.id == pageId) {
                    page.copy(annotations = page.annotations.dropLast(1))
                } else {
                    page
                }
            }
            val currentRedoList = currentState.redoAnnotationsMap[pageId] ?: emptyList()
            val updatedRedo = currentState.redoAnnotationsMap + (pageId to (currentRedoList + annotationToUndo))
            currentState.copy(pages = updatedPages, redoAnnotationsMap = updatedRedo)
        }
    }

    fun redoPageAnnotation(pageId: String) {
        _uiState.update { currentState ->
            val redoList = currentState.redoAnnotationsMap[pageId] ?: return@update currentState
            if (redoList.isEmpty()) return@update currentState

            val annotationToRedo = redoList.last()
            val updatedPages = currentState.pages.map { page ->
                if (page.id == pageId) {
                    page.copy(annotations = page.annotations + annotationToRedo)
                } else {
                    page
                }
            }
            val updatedRedo = currentState.redoAnnotationsMap + (pageId to redoList.dropLast(1))
            currentState.copy(pages = updatedPages, redoAnnotationsMap = updatedRedo)
        }
    }

    fun clearPageAnnotations(pageId: String) {
        _uiState.update { currentState ->
            val targetPage = currentState.pages.find { it.id == pageId } ?: return@update currentState
            if (targetPage.annotations.isEmpty()) return@update currentState

            val currentRedoList = currentState.redoAnnotationsMap[pageId] ?: emptyList()
            val updatedRedo = currentState.redoAnnotationsMap + (pageId to (currentRedoList + targetPage.annotations))
            val updatedPages = currentState.pages.map { page ->
                if (page.id == pageId) {
                    page.copy(annotations = emptyList())
                } else {
                    page
                }
            }
            currentState.copy(pages = updatedPages, redoAnnotationsMap = updatedRedo)
        }
    }

    fun setGridView(enabled: Boolean) {
        _uiState.update { it.copy(isGridView = enabled) }
    }

    fun updateFileName(name: String) {
        _uiState.update { currentState ->
            currentState.copy(options = currentState.options.copy(fileName = name))
        }
    }

    fun updatePageSize(pageSize: PdfPageSize) {
        _uiState.update { currentState ->
            currentState.copy(options = currentState.options.copy(pageSize = pageSize))
        }
    }

    fun updateOrientation(orientation: PdfPageOrientation) {
        _uiState.update { currentState ->
            currentState.copy(options = currentState.options.copy(orientation = orientation))
        }
    }

    fun updateCompressionProfile(profile: CompressionProfile) {
        _uiState.update { currentState ->
            currentState.copy(options = currentState.options.copy(compressionProfile = profile))
        }
    }

    fun showOptionsBottomSheet(visible: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isOptionsBottomSheetVisible = visible)
        }
    }

    fun dismissResultDialog() {
        _uiState.update { currentState ->
            currentState.copy(conversionResult = null)
        }
    }

    fun dismissError() {
        _uiState.update { currentState ->
            currentState.copy(errorMessage = null)
        }
    }

    fun convertImagesToPdf() {
        val pages = _uiState.value.pages
        if (pages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select at least one photo first.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConverting = true,
                    conversionProgress = ConversionProgress(currentPageIndex = 0, totalPages = pages.size),
                    errorMessage = null
                )
            }

            pdfConversionRepository.convertImagesToPdf(
                pages = pages,
                options = _uiState.value.options,
                destinationFolderUri = _uiState.value.destinationFolderUri,
                onProgress = { progress ->
                    _uiState.update { it.copy(conversionProgress = progress) }
                }
            ).collect { result ->
                when (result) {
                    is ResourceResult.Loading -> {
                        _uiState.update { it.copy(isConverting = true) }
                    }
                    is ResourceResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isConverting = false,
                                conversionProgress = null,
                                conversionResult = result.data
                            )
                        }
                        loadRecentPdfs()
                    }
                    is ResourceResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isConverting = false,
                                conversionProgress = null,
                                errorMessage = result.userMessage ?: "Failed to create PDF document."
                            )
                        }
                    }
                }
            }
        }
    }

    private fun queryUriMetadata(uri: Uri, contentResolver: ContentResolver): UriMetadata {
        var name: String? = null
        var size: Long? = null
        var width = 0
        var height = 0

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex)
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {
            // Keep nulls if querying content provider fails
        }

        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, opts)
                if (opts.outWidth > 0 && opts.outHeight > 0) {
                    width = opts.outWidth
                    height = opts.outHeight
                }
            }
        } catch (_: Exception) {
            // Keep 0 if reading image stream fails
        }

        return UriMetadata(displayName = name, sizeInBytes = size, width = width, height = height)
    }

    private data class UriMetadata(
        val displayName: String?,
        val sizeInBytes: Long?,
        val width: Int = 0,
        val height: Int = 0
    )

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val decoder = ImageDecoderDataSource(appContext)
                    val generator = PdfGeneratorDataSource(decoder)
                    val repository = PdfConversionRepositoryImpl(appContext, generator)
                    val recentPdfsRepo = RecentPdfsRepository(appContext)
                    val presetRepo = PresetRepository(appContext)
                    val settingsRepo = SettingsRepository(appContext)
                    return ImagesToPdfViewModel(repository, recentPdfsRepo, presetRepo, settingsRepo) as T
                }
            }
    }
}
