package com.leshoraa.scanorea.features.editor.ui

import android.content.ContentResolver
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.core.filter.ImageFilterType
import com.leshoraa.scanorea.features.editor.domain.model.AnnotationColors
import com.leshoraa.scanorea.features.editor.domain.model.AnnotationTool
import com.leshoraa.scanorea.features.editor.domain.model.DocumentQuad
import com.leshoraa.scanorea.features.editor.domain.model.EditorCategory
import com.leshoraa.scanorea.features.editor.domain.model.PageAnnotation
import com.leshoraa.scanorea.features.editor.domain.model.StrokeSize
import com.leshoraa.scanorea.features.editor.ui.components.CropPanelMode
import com.leshoraa.scanorea.features.editor.ui.components.EditorAdjustPanel
import com.leshoraa.scanorea.features.editor.ui.components.EditorAnnotatePanel
import com.leshoraa.scanorea.features.editor.ui.components.EditorBottomActionBar
import com.leshoraa.scanorea.features.editor.ui.components.EditorCanvasPager
import com.leshoraa.scanorea.features.editor.ui.components.EditorCategoryTabBar
import com.leshoraa.scanorea.features.editor.ui.components.EditorCropPanel
import com.leshoraa.scanorea.features.editor.ui.components.EditorFiltersPanel
import com.leshoraa.scanorea.features.imagestopdf.domain.model.CropAspectRatio
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImageCropBounds
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Full-screen document editing workspace.
 * Coordinates canvas pagination, interactive cropping, non-modal adjustments,
 * category tabs, and PDF conversion triggers.
 */
@Composable
fun EditorWorkspace(
    pages: List<ImagePage>,
    pagerState: PagerState,
    estimatedSizeBytes: Long,
    onMovePage: (sourceIndex: Int, targetIndex: Int) -> Unit,
    onRemovePage: (String) -> Unit,
    onFilterSelected: (pageId: String, filter: ImageFilterType) -> Unit,
    onApplyFilterToAll: (filter: ImageFilterType) -> Unit,
    onAdjustmentChange: (pageId: String, contrast: Float, brightness: Float) -> Unit,
    onAutoAdjustPage: (pageId: String, contentResolver: ContentResolver) -> Unit,
    onResetAdjustments: (pageId: String) -> Unit,
    onCropChange: (pageId: String, bounds: ImageCropBounds) -> Unit,
    onPerspectiveQuadChange: (pageId: String, quad: DocumentQuad) -> Unit = { _, _ -> },
    onAutoDetectPerspective: (pageId: String) -> Unit = {},
    onResetPerspective: (pageId: String) -> Unit = {},
    onRotatePage: (pageId: String) -> Unit,
    onResetCrop: (pageId: String) -> Unit,
    canUndoAnnotation: Boolean = false,
    canRedoAnnotation: Boolean = false,
    onAddAnnotation: (pageId: String, annotation: PageAnnotation) -> Unit = { _, _ -> },
    onUndoAnnotation: (pageId: String) -> Unit = {},
    onRedoAnnotation: (pageId: String) -> Unit = {},
    onClearAnnotations: (pageId: String) -> Unit = {},
    onCancel: () -> Unit,
    onOpenOptionsSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (pages.isEmpty()) return

    val currentPageIndex = pagerState.currentPage.coerceIn(0, pages.size - 1)
    val currentPage = pages.getOrNull(currentPageIndex)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeCategory by remember { mutableStateOf(EditorCategory.FILTERS) }

    var selectedAnnotationTool by remember { mutableStateOf(AnnotationTool.PEN) }
    var selectedAnnotationColor by remember { mutableLongStateOf(AnnotationColors.PRESETS.first()) }
    var selectedAnnotationAlpha by remember { mutableFloatStateOf(1.0f) }
    var selectedStrokeSize by remember { mutableStateOf(StrokeSize.MEDIUM) }

    var isReordering by remember { mutableStateOf(false) }
    var movingPage by remember { mutableStateOf<ImagePage?>(null) }
    var movingTargetIndex by remember { mutableIntStateOf(-1) }
    val liftAnim = remember { Animatable(0f) }

    var selectedCropRatio by remember(currentPage?.id) { mutableStateOf(CropAspectRatio.FREE) }
    var cropPanelMode by remember { mutableStateOf(CropPanelMode.RECTANGLE) }
    var originalCropBoundsOnEnter by remember { mutableStateOf(ImageCropBounds.DEFAULT) }
    var originalPerspectiveQuadOnEnter by remember { mutableStateOf(DocumentQuad.DEFAULT) }

    LaunchedEffect(activeCategory) {
        if (activeCategory == EditorCategory.CROP) {
            val curr = pages.getOrNull(pagerState.currentPage)
            originalCropBoundsOnEnter = curr?.cropBounds ?: ImageCropBounds.DEFAULT
            originalPerspectiveQuadOnEnter = curr?.perspectiveQuad ?: DocumentQuad.DEFAULT
            cropPanelMode = CropPanelMode.RECTANGLE
        } else {
            selectedCropRatio = CropAspectRatio.FREE
            cropPanelMode = CropPanelMode.RECTANGLE
        }
    }

    val handleApplyCrop: () -> Unit = {
        selectedCropRatio = CropAspectRatio.FREE
        cropPanelMode = CropPanelMode.RECTANGLE
        activeCategory = EditorCategory.FILTERS
    }

    val handleCancelCrop: () -> Unit = {
        val curr = pages.getOrNull(pagerState.currentPage)
        if (curr != null) {
            onCropChange(curr.id, originalCropBoundsOnEnter)
            onPerspectiveQuadChange(curr.id, originalPerspectiveQuadOnEnter)
        }
        selectedCropRatio = CropAspectRatio.FREE
        cropPanelMode = CropPanelMode.RECTANGLE
        activeCategory = EditorCategory.FILTERS
    }

    fun startMoveAnimation(sourceIndex: Int, targetIndex: Int) {
        if (isReordering || sourceIndex == targetIndex || sourceIndex !in pages.indices || targetIndex !in pages.indices) return
        val pageToMove = pages[sourceIndex]
        coroutineScope.launch {
            isReordering = true
            movingTargetIndex = targetIndex
            movingPage = pageToMove

            // Phase 1: Lift card
            liftAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )

            // Phase 2: Scroll to destination
            val distance = abs(targetIndex - sourceIndex)
            val scrollDuration = (320 + distance * 130).coerceIn(380, 800)
            pagerState.animateScrollToPage(
                page = targetIndex,
                animationSpec = tween(durationMillis = scrollDuration, easing = FastOutSlowInEasing)
            )

            // Phase 3: Swap data
            onMovePage(sourceIndex, targetIndex)
            pagerState.scrollToPage(targetIndex)

            // Phase 4: Settle card
            liftAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            )
            pagerState.scrollToPage(targetIndex)

            isReordering = false
            movingPage = null
            movingTargetIndex = -1
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Image Canvas & Pager Viewport
        EditorCanvasPager(
            pages = pages,
            pagerState = pagerState,
            isReordering = isReordering,
            movingPage = movingPage,
            movingTargetIndex = movingTargetIndex,
            liftProgress = liftAnim.value,
            isCropMode = activeCategory == EditorCategory.CROP,
            cropPanelMode = cropPanelMode,
            cropAspectRatio = selectedCropRatio,
            onCropChange = onCropChange,
            onPerspectiveQuadChange = onPerspectiveQuadChange,
            isAnnotateMode = activeCategory == EditorCategory.MARKUP,
            activeAnnotationTool = selectedAnnotationTool,
            selectedAnnotationColor = selectedAnnotationColor,
            selectedAnnotationAlpha = selectedAnnotationAlpha,
            selectedStrokeSize = selectedStrokeSize,
            canUndoAnnotation = canUndoAnnotation,
            canRedoAnnotation = canRedoAnnotation,
            onAddAnnotation = onAddAnnotation,
            onUndoAnnotation = { currentPage?.let { onUndoAnnotation(it.id) } },
            onRedoAnnotation = { currentPage?.let { onRedoAnnotation(it.id) } },
            onClearAnnotations = { currentPage?.let { onClearAnnotations(it.id) } },
            modifier = Modifier.weight(1f)
        )

        // Contextual Editing Tool Panel
        if (currentPage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = activeCategory,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label = "EditorCategoryContent"
                ) { category ->
                    when (category) {
                        EditorCategory.FILTERS -> {
                            EditorFiltersPanel(
                                selectedFilter = currentPage.filter,
                                onFilterSelected = { onFilterSelected(currentPage.id, it) },
                                onApplyToAll = { onApplyFilterToAll(currentPage.filter) }
                            )
                        }
                        EditorCategory.ADJUST -> {
                            EditorAdjustPanel(
                                page = currentPage,
                                onAutoAdjust = { onAutoAdjustPage(currentPage.id, context.contentResolver) },
                                onResetAdjustments = { onResetAdjustments(currentPage.id) },
                                onAdjustmentChange = { contrast, brightness ->
                                    onAdjustmentChange(currentPage.id, contrast, brightness)
                                }
                            )
                        }
                        EditorCategory.CROP -> {
                            EditorCropPanel(
                                page = currentPage,
                                currentPageIndex = currentPageIndex,
                                totalPages = pages.size,
                                isReordering = isReordering,
                                cropPanelMode = cropPanelMode,
                                onCropPanelModeChange = { cropPanelMode = it },
                                selectedRatio = selectedCropRatio,
                                onRatioSelected = { selectedCropRatio = it },
                                onCropBoundsChange = { newBounds -> onCropChange(currentPage.id, newBounds) },
                                onRotatePage = { onRotatePage(currentPage.id) },
                                onResetCrop = { onResetCrop(currentPage.id) },
                                onAutoDetectPerspective = { onAutoDetectPerspective(currentPage.id) },
                                onResetPerspective = { onResetPerspective(currentPage.id) },
                                onApplyCrop = handleApplyCrop,
                                onPreviousPage = {
                                    coroutineScope.launch {
                                        if (pagerState.currentPage > 0) {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                },
                                onNextPage = {
                                    coroutineScope.launch {
                                        if (pagerState.currentPage < pages.size - 1) {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                },
                                onMovePage = { src, tgt -> startMoveAnimation(src, tgt) }
                            )
                        }
                        EditorCategory.MARKUP -> {
                            EditorAnnotatePanel(
                                selectedTool = selectedAnnotationTool,
                                onToolSelected = { selectedAnnotationTool = it },
                                selectedColor = selectedAnnotationColor,
                                onColorSelected = { selectedAnnotationColor = it },
                                selectedAlpha = selectedAnnotationAlpha,
                                onAlphaSelected = { selectedAnnotationAlpha = it },
                                selectedStrokeSize = selectedStrokeSize,
                                onStrokeSizeSelected = { selectedStrokeSize = it }
                            )
                        }
                    }
                }
            }
        }

        // Category Tab Bar
        EditorCategoryTabBar(
            selectedCategory = activeCategory,
            onCategorySelected = { activeCategory = it }
        )

        // Bottom Action Bar
        EditorBottomActionBar(
            estimatedSizeBytes = estimatedSizeBytes,
            onCancel = onCancel,
            onConvertToPdf = onOpenOptionsSheet,
            isCropMode = activeCategory == EditorCategory.CROP,
            onCancelCrop = handleCancelCrop,
            onApplyCrop = handleApplyCrop
        )
    }
}
