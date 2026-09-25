package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.leshoraa.scanorea.features.editor.domain.CropGestureCalculator
import com.leshoraa.scanorea.features.editor.domain.model.AnnotationColors
import com.leshoraa.scanorea.features.editor.domain.model.AnnotationTool
import com.leshoraa.scanorea.features.editor.domain.model.DocumentQuad
import com.leshoraa.scanorea.features.editor.domain.model.NormalizedPoint
import com.leshoraa.scanorea.features.editor.domain.model.PageAnnotation
import com.leshoraa.scanorea.features.editor.domain.model.StrokeSize
import com.leshoraa.scanorea.features.imagestopdf.domain.model.CropAspectRatio
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImageCropBounds
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Image pager viewport displaying pages with rotation, cropping, filter indicators,
 * interactive crop overlays, smooth reordering animations, and hardware-accelerated zoom & pan.
 */
@Composable
fun EditorCanvasPager(
    pages: List<ImagePage>,
    pagerState: PagerState,
    isReordering: Boolean,
    movingPage: ImagePage?,
    movingTargetIndex: Int,
    liftProgress: Float,
    isCropMode: Boolean,
    cropPanelMode: CropPanelMode = CropPanelMode.RECTANGLE,
    cropAspectRatio: CropAspectRatio = CropAspectRatio.FREE,
    stagedCropBounds: ImageCropBounds = ImageCropBounds.DEFAULT,
    stagedPerspectiveQuad: DocumentQuad = DocumentQuad.DEFAULT,
    onStagedCropBoundsChange: (ImageCropBounds) -> Unit = {},
    onStagedPerspectiveQuadChange: (DocumentQuad) -> Unit = {},
    onCropChange: (pageId: String, bounds: ImageCropBounds) -> Unit = { _, _ -> },
    onPerspectiveQuadChange: (pageId: String, quad: DocumentQuad) -> Unit = { _, _ -> },
    isAnnotateMode: Boolean = false,
    activeAnnotationTool: AnnotationTool = AnnotationTool.PEN,
    selectedAnnotationColor: Long = AnnotationColors.YELLOW,
    selectedAnnotationAlpha: Float = 1.0f,
    selectedStrokeSize: StrokeSize = StrokeSize.MEDIUM,
    canUndoAnnotation: Boolean = false,
    canRedoAnnotation: Boolean = false,
    onAddAnnotation: (pageId: String, annotation: PageAnnotation) -> Unit = { _, _ -> },
    onUndoAnnotation: () -> Unit = {},
    onRedoAnnotation: () -> Unit = {},
    onClearAnnotations: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeZoomScale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(pagerState.currentPage, isCropMode, isAnnotateMode) {
        activeZoomScale = 1f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isReordering && !isCropMode && !isAnnotateMode && (activeZoomScale <= 1.05f)
        ) { pageIndex ->
            val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
            val colorMatrix = page.filter.createColorMatrix(page.contrast, page.brightness)
            val colorFilter = colorMatrix?.let { ColorFilter.colorMatrix(ColorMatrix(it.array)) }
            val isBeingMoved = isReordering && page.id == movingPage?.id
            val isCurrentFocusedPage = pageIndex == pagerState.currentPage

            val coroutineScope = rememberCoroutineScope()
            val currentOnCropChange by rememberUpdatedState(onCropChange)
            val currentIsAnnotateMode by rememberUpdatedState(isAnnotateMode)
            val currentAnnotationTool by rememberUpdatedState(activeAnnotationTool)
            val currentAnnotationColor by rememberUpdatedState(selectedAnnotationColor)
            val currentAnnotationAlpha by rememberUpdatedState(selectedAnnotationAlpha)
            val currentStrokeSize by rememberUpdatedState(selectedStrokeSize)
            val currentOnAddAnnotation by rememberUpdatedState(onAddAnnotation)

            var liveInProgressAnnotation by remember { mutableStateOf<PageAnnotation?>(null) }

            LaunchedEffect(isAnnotateMode, page.id) {
                liveInProgressAnnotation = null
            }

            var zoomScale by remember(page.id, isCropMode) { mutableFloatStateOf(1f) }
            var panOffset by remember(page.id, isCropMode) { mutableStateOf(Offset.Zero) }
            var zoomAnimJob by remember { mutableStateOf<Job?>(null) }

            fun animateZoom(targetScale: Float, targetPan: Offset) {
                zoomAnimJob?.cancel()
                zoomAnimJob = coroutineScope.launch {
                    val startScale = zoomScale
                    val startPan = panOffset
                    animate(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                    ) { progress, _ ->
                        zoomScale = startScale + (targetScale - startScale) * progress
                        panOffset = Offset(
                            startPan.x + (targetPan.x - startPan.x) * progress,
                            startPan.y + (targetPan.y - startPan.y) * progress
                        )
                        activeZoomScale = zoomScale
                    }
                }
            }

            LaunchedEffect(isCurrentFocusedPage) {
                if (!isCurrentFocusedPage) {
                    zoomAnimJob?.cancel()
                    zoomScale = 1f
                    panOffset = Offset.Zero
                }
            }

            var isBadgeVisible by remember(page.id, page.filter) { mutableStateOf(true) }
            LaunchedEffect(page.id, page.filter) {
                isBadgeVisible = true
                delay(1800)
                isBadgeVisible = false
            }

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isBeingMoved) 0f else 1f)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    var imageAspectRatio by remember(page.id, page.rotationDegrees) { mutableFloatStateOf(0f) }

                    // Determine transformation based on active mode
                    val effectiveCropBounds = if (isCropMode && isCurrentFocusedPage) {
                        ImageCropBounds.DEFAULT
                    } else {
                        page.cropBounds
                    }

                    // Viewport breathing room in Crop mode so edges & corners are never cramped against screen bounds
                    val cropInset by animateDpAsState(
                        targetValue = if (isCropMode) 28.dp else 0.dp,
                        animationSpec = tween(250),
                        label = "cropInset"
                    )

                    val availableW = (maxWidth - cropInset * 2).coerceAtLeast(100.dp)
                    val availableH = (maxHeight - cropInset * 2).coerceAtLeast(100.dp)

                    val (fittedW, fittedH) = if (imageAspectRatio > 0f) {
                        val containerAspect = availableW / availableH
                        if (imageAspectRatio > containerAspect) {
                            availableW to (availableW / imageAspectRatio)
                        } else {
                            (availableH * imageAspectRatio) to availableH
                        }
                    } else {
                        availableW to availableH
                    }

                    val touchMargin = 24.dp

                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val fittedWPx = with(density) { fittedW.toPx() }
                    val fittedHPx = with(density) { fittedH.toPx() }

                    val haptic = LocalHapticFeedback.current
                    val localCropBoundsState = remember(page.id) {
                        mutableStateOf(if (isCropMode && isCurrentFocusedPage) stagedCropBounds else page.cropBounds)
                    }
                    var activeCropHandle by remember { mutableStateOf(DragHandle.NONE) }
                    val currentCropAspectRatio by rememberUpdatedState(cropAspectRatio)
                    val currentEffectiveAspectRatio by rememberUpdatedState(page.effectiveAspectRatio)

                    LaunchedEffect(page.cropBounds, stagedCropBounds, isCropMode, isCurrentFocusedPage) {
                        if (activeCropHandle == DragHandle.NONE) {
                            val target = if (isCropMode && isCurrentFocusedPage) stagedCropBounds else page.cropBounds
                            if (localCropBoundsState.value != target) {
                                localCropBoundsState.value = target
                            }
                        }
                    }

                    LaunchedEffect(isCropMode) {
                        if (!isCropMode) {
                            activeCropHandle = DragHandle.NONE
                        }
                    }

                    // Static Viewport Container - Pointer gestures are collected here on unscaled,
                    // unpanned viewport coordinates, eliminating graphicsLayer matrix feedback loops & jitter.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(page.id, isCropMode, isAnnotateMode, isCurrentFocusedPage, fittedWPx, fittedHPx) {
                                if (!isCurrentFocusedPage) return@pointerInput

                                val cornerHitRadius = 34.dp.toPx()
                                val edgeHitRadius = 42.dp.toPx()
                                val edgePad = 16.dp.toPx()

                                var lastTapTime = 0L
                                var lastTapPos = Offset.Zero

                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    zoomAnimJob?.cancel()
                                    val startPos = down.position
                                    val startTime = System.currentTimeMillis()

                                    val viewW = size.width.toFloat()
                                    val viewH = size.height.toFloat()
                                    val centerX = viewW / 2f
                                    val centerY = viewH / 2f

                                    val imgCenterX = centerX + panOffset.x
                                    val imgCenterY = centerY + panOffset.y
                                    val imgScreenW = fittedWPx * zoomScale
                                    val imgScreenH = fittedHPx * zoomScale
                                    val imgScreenLeft = imgCenterX - (imgScreenW / 2f)
                                    val imgScreenTop = imgCenterY - (imgScreenH / 2f)

                                    fun toNorm(p: Offset): NormalizedPoint {
                                        val nx = if (imgScreenW > 0f) ((p.x - imgScreenLeft) / imgScreenW).coerceIn(0f, 1f) else 0f
                                        val ny = if (imgScreenH > 0f) ((p.y - imgScreenTop) / imgScreenH).coerceIn(0f, 1f) else 0f
                                        return NormalizedPoint(nx, ny)
                                    }

                                    var isAnnotating = false
                                    var startNorm = NormalizedPoint(0f, 0f)
                                    var activePoints = mutableListOf<NormalizedPoint>()
                                    var activeAnno: PageAnnotation? = null

                                    if (currentIsAnnotateMode && currentAnnotationTool != AnnotationTool.NAVIGATE) {
                                        val isInsideImage = startPos.x in (imgScreenLeft - 24f)..(imgScreenLeft + imgScreenW + 24f) &&
                                                startPos.y in (imgScreenTop - 24f)..(imgScreenTop + imgScreenH + 24f)
                                        if (isInsideImage) {
                                            isAnnotating = true
                                            startNorm = toNorm(startPos)
                                            activePoints = mutableListOf(startNorm)

                                            activeAnno = when (currentAnnotationTool) {
                                                AnnotationTool.NAVIGATE -> null
                                                AnnotationTool.PEN -> PageAnnotation.FreehandPath(
                                                    color = currentAnnotationColor,
                                                    strokeWidth = currentStrokeSize.widthDp,
                                                    alpha = currentAnnotationAlpha,
                                                    points = activePoints.toList()
                                                )
                                                AnnotationTool.HIGHLIGHTER -> PageAnnotation.RectBox(
                                                    color = currentAnnotationColor,
                                                    strokeWidth = 0f,
                                                    left = startNorm.x,
                                                    top = startNorm.y,
                                                    right = startNorm.x,
                                                    bottom = startNorm.y,
                                                    isFilled = true,
                                                    alpha = if (currentAnnotationAlpha < 1.0f) currentAnnotationAlpha else 0.38f
                                                )
                                                AnnotationTool.REDACT -> PageAnnotation.RectBox(
                                                    color = 0xFF111111,
                                                    strokeWidth = 0f,
                                                    left = startNorm.x,
                                                    top = startNorm.y,
                                                    right = startNorm.x,
                                                    bottom = startNorm.y,
                                                    isFilled = true,
                                                    alpha = 1.0f
                                                )
                                                AnnotationTool.RECT_SHAPE -> PageAnnotation.RectBox(
                                                    color = currentAnnotationColor,
                                                    strokeWidth = currentStrokeSize.widthDp,
                                                    left = startNorm.x,
                                                    top = startNorm.y,
                                                    right = startNorm.x,
                                                    bottom = startNorm.y,
                                                    isFilled = false,
                                                    alpha = currentAnnotationAlpha
                                                )
                                                AnnotationTool.OVAL_SHAPE -> PageAnnotation.OvalShape(
                                                    color = currentAnnotationColor,
                                                    strokeWidth = currentStrokeSize.widthDp,
                                                    left = startNorm.x,
                                                    top = startNorm.y,
                                                    right = startNorm.x,
                                                    bottom = startNorm.y,
                                                    alpha = currentAnnotationAlpha
                                                )
                                                AnnotationTool.ARROW_SHAPE -> PageAnnotation.ArrowLine(
                                                    color = currentAnnotationColor,
                                                    strokeWidth = currentStrokeSize.widthDp,
                                                    startX = startNorm.x,
                                                    startY = startNorm.y,
                                                    endX = startNorm.x,
                                                    endY = startNorm.y,
                                                    alpha = currentAnnotationAlpha
                                                )
                                            }
                                            liveInProgressAnnotation = activeAnno
                                        }
                                    }

                                    var handle = DragHandle.NONE

                                    if (isCropMode && cropPanelMode != CropPanelMode.PERSPECTIVE) {
                                        val bounds = localCropBoundsState.value
                                        val screenLeft = centerX + (bounds.left - 0.5f) * fittedWPx * zoomScale + panOffset.x
                                        val screenRight = centerX + (bounds.right - 0.5f) * fittedWPx * zoomScale + panOffset.x
                                        val screenTop = centerY + (bounds.top - 0.5f) * fittedHPx * zoomScale + panOffset.y
                                        val screenBottom = centerY + (bounds.bottom - 0.5f) * fittedHPx * zoomScale + panOffset.y

                                        val x = startPos.x
                                        val y = startPos.y

                                        val dTL = hypot(x - screenLeft, y - screenTop)
                                        val dTR = hypot(x - screenRight, y - screenTop)
                                        val dBL = hypot(x - screenLeft, y - screenBottom)
                                        val dBR = hypot(x - screenRight, y - screenBottom)

                                        val nearTop = x in (screenLeft - edgePad)..(screenRight + edgePad)
                                        val dTop = if (nearTop) kotlin.math.abs(y - screenTop) else Float.MAX_VALUE

                                        val nearBottom = x in (screenLeft - edgePad)..(screenRight + edgePad)
                                        val dBottom = if (nearBottom) kotlin.math.abs(y - screenBottom) else Float.MAX_VALUE

                                        val nearLeft = y in (screenTop - edgePad)..(screenBottom + edgePad)
                                        val dLeft = if (nearLeft) kotlin.math.abs(x - screenLeft) else Float.MAX_VALUE

                                        val nearRight = y in (screenTop - edgePad)..(screenBottom + edgePad)
                                        val dRight = if (nearRight) kotlin.math.abs(x - screenRight) else Float.MAX_VALUE

                                        val candidates = mutableListOf<Pair<DragHandle, Float>>()
                                        if (dTL < cornerHitRadius) candidates.add(DragHandle.TOP_LEFT to dTL)
                                        if (dTR < cornerHitRadius) candidates.add(DragHandle.TOP_RIGHT to dTR)
                                        if (dBL < cornerHitRadius) candidates.add(DragHandle.BOTTOM_LEFT to dBL)
                                        if (dBR < cornerHitRadius) candidates.add(DragHandle.BOTTOM_RIGHT to dBR)

                                        if (dTop < edgeHitRadius) candidates.add(DragHandle.TOP_EDGE to dTop)
                                        if (dBottom < edgeHitRadius) candidates.add(DragHandle.BOTTOM_EDGE to dBottom)
                                        if (dLeft < edgeHitRadius) candidates.add(DragHandle.LEFT_EDGE to dLeft)
                                        if (dRight < edgeHitRadius) candidates.add(DragHandle.RIGHT_EDGE to dRight)

                                        handle = candidates.minByOrNull { it.second }?.first ?: DragHandle.NONE
                                        if (handle == DragHandle.NONE && x in screenLeft..screenRight && y in screenTop..screenBottom) {
                                            handle = DragHandle.BODY
                                        }
                                        if (handle != DragHandle.NONE) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                    activeCropHandle = handle

                                    var isPinch = false
                                    var prevSpan = 0f
                                    var prevCentroid = Offset.Zero
                                    var totalDragDistance = 0f

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val pressed = event.changes.filter { it.pressed }
                                        if (pressed.isEmpty()) {
                                            if (isAnnotating && activeAnno != null && !isPinch) {
                                                val finalAnno = activeAnno
                                                val isMicroTap = when (finalAnno) {
                                                    is PageAnnotation.RectBox -> kotlin.math.abs(finalAnno.right - finalAnno.left) < 0.005f && kotlin.math.abs(finalAnno.bottom - finalAnno.top) < 0.005f
                                                    is PageAnnotation.OvalShape -> kotlin.math.abs(finalAnno.right - finalAnno.left) < 0.005f && kotlin.math.abs(finalAnno.bottom - finalAnno.top) < 0.005f
                                                    is PageAnnotation.ArrowLine -> hypot(finalAnno.endX - finalAnno.startX, finalAnno.endY - finalAnno.startY) < 0.01f
                                                    is PageAnnotation.FreehandPath -> false
                                                }
                                                if (!isMicroTap) {
                                                    currentOnAddAnnotation(page.id, finalAnno)
                                                }
                                                activeAnno = null
                                                liveInProgressAnnotation = null
                                                isAnnotating = false
                                            }

                                            val duration = System.currentTimeMillis() - startTime
                                            if (handle == DragHandle.NONE && !currentIsAnnotateMode && totalDragDistance < 15f && duration < 300L) {
                                                val now = System.currentTimeMillis()
                                                if (now - lastTapTime < 350L && hypot(startPos.x - lastTapPos.x, startPos.y - lastTapPos.y) < 60.dp.toPx()) {
                                                    if (zoomScale > 1.05f) {
                                                        animateZoom(1f, Offset.Zero)
                                                    } else {
                                                        val targetScale = 2.5f
                                                        val maxPanX = ((fittedWPx * (targetScale - 1f)) / 2f).coerceAtLeast(0f)
                                                        val maxPanY = ((fittedHPx * (targetScale - 1f)) / 2f).coerceAtLeast(0f)
                                                        val cRel = startPos - Offset(centerX, centerY)
                                                        val targetPan = Offset(
                                                            x = (-cRel.x * 1.5f).coerceIn(-maxPanX, maxPanX),
                                                            y = (-cRel.y * 1.5f).coerceIn(-maxPanY, maxPanY)
                                                        )
                                                        animateZoom(targetScale, targetPan)
                                                    }
                                                    lastTapTime = 0L
                                                } else {
                                                    lastTapTime = now
                                                    lastTapPos = startPos
                                                    if (!isCropMode && !currentIsAnnotateMode) {
                                                        isBadgeVisible = !isBadgeVisible
                                                    }
                                                }
                                            }

                                            if (activeCropHandle != DragHandle.NONE) {
                                                onStagedCropBoundsChange(localCropBoundsState.value)
                                            }
                                            activeCropHandle = DragHandle.NONE
                                            break
                                        }

                                        if (pressed.size >= 2) {
                                            isPinch = true
                                            activeCropHandle = DragHandle.NONE
                                            if (isAnnotating) {
                                                isAnnotating = false
                                                activeAnno = null
                                                liveInProgressAnnotation = null
                                            }

                                            val p0 = pressed[0].position
                                            val p1 = pressed[1].position
                                            val currentSpan = hypot(p0.x - p1.x, p0.y - p1.y)
                                            val currentCentroid = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)

                                            if (prevSpan > 0f && currentSpan > 0f) {
                                                val zoomFactor = currentSpan / prevSpan
                                                val panChange = currentCentroid - prevCentroid
                                                val oldScale = zoomScale
                                                val newScale = (oldScale * zoomFactor).coerceIn(1f, 4f)
                                                val actualFactor = newScale / oldScale
                                                val cRel = currentCentroid - Offset(centerX, centerY)

                                                panOffset = (panOffset * actualFactor) + (cRel * (1f - actualFactor)) + panChange
                                                zoomScale = newScale
                                                activeZoomScale = newScale

                                                if (newScale > 1.02f) {
                                                    val maxPanX = ((fittedWPx * (newScale - 1f)) / 2f).coerceAtLeast(0f)
                                                    val maxPanY = ((fittedHPx * (newScale - 1f)) / 2f).coerceAtLeast(0f)
                                                    panOffset = Offset(
                                                        x = panOffset.x.coerceIn(-maxPanX, maxPanX),
                                                        y = panOffset.y.coerceIn(-maxPanY, maxPanY)
                                                    )
                                                } else {
                                                    zoomScale = 1f
                                                    activeZoomScale = 1f
                                                    panOffset = Offset.Zero
                                                }
                                            }
                                            prevSpan = currentSpan
                                            prevCentroid = currentCentroid
                                            event.changes.forEach { it.consume() }
                                        } else if (pressed.size == 1 && !isPinch) {
                                            val change = pressed[0]
                                            val dragAmount = change.positionChange()
                                            totalDragDistance += hypot(dragAmount.x, dragAmount.y)

                                            if (isAnnotating && activeAnno != null) {
                                                change.consume()
                                                val currNorm = toNorm(change.position)
                                                activeAnno = when (currentAnnotationTool) {
                                                    AnnotationTool.NAVIGATE -> null
                                                    AnnotationTool.PEN -> {
                                                        activePoints.add(currNorm)
                                                        (activeAnno as? PageAnnotation.FreehandPath)?.copy(points = activePoints.toList())
                                                    }
                                                    AnnotationTool.HIGHLIGHTER -> {
                                                        (activeAnno as? PageAnnotation.RectBox)?.copy(
                                                            left = min(startNorm.x, currNorm.x),
                                                            top = min(startNorm.y, currNorm.y),
                                                            right = max(startNorm.x, currNorm.x),
                                                            bottom = max(startNorm.y, currNorm.y)
                                                        )
                                                    }
                                                    AnnotationTool.REDACT -> {
                                                        (activeAnno as? PageAnnotation.RectBox)?.copy(
                                                            left = min(startNorm.x, currNorm.x),
                                                            top = min(startNorm.y, currNorm.y),
                                                            right = max(startNorm.x, currNorm.x),
                                                            bottom = max(startNorm.y, currNorm.y)
                                                        )
                                                    }
                                                    AnnotationTool.RECT_SHAPE -> {
                                                        (activeAnno as? PageAnnotation.RectBox)?.copy(
                                                            left = min(startNorm.x, currNorm.x),
                                                            top = min(startNorm.y, currNorm.y),
                                                            right = max(startNorm.x, currNorm.x),
                                                            bottom = max(startNorm.y, currNorm.y)
                                                        )
                                                    }
                                                    AnnotationTool.OVAL_SHAPE -> {
                                                        (activeAnno as? PageAnnotation.OvalShape)?.copy(
                                                            left = min(startNorm.x, currNorm.x),
                                                            top = min(startNorm.y, currNorm.y),
                                                            right = max(startNorm.x, currNorm.x),
                                                            bottom = max(startNorm.y, currNorm.y)
                                                        )
                                                    }
                                                    AnnotationTool.ARROW_SHAPE -> {
                                                        (activeAnno as? PageAnnotation.ArrowLine)?.copy(
                                                            startX = startNorm.x,
                                                            startY = startNorm.y,
                                                            endX = currNorm.x,
                                                            endY = currNorm.y
                                                        )
                                                    }
                                                }
                                                liveInProgressAnnotation = activeAnno
                                            } else if (activeCropHandle != DragHandle.NONE) {
                                                change.consume()
                                                val deltaX = dragAmount.x / (fittedWPx * zoomScale)
                                                val deltaY = dragAmount.y / (fittedHPx * zoomScale)
                                                val minSize = 0.08f
                                                val newBounds = CropGestureCalculator.updateBounds(
                                                    currentBounds = localCropBoundsState.value,
                                                    activeHandle = activeCropHandle,
                                                    deltaX = deltaX,
                                                    deltaY = deltaY,
                                                    cropAspectRatio = currentCropAspectRatio,
                                                    effectiveAspectRatio = currentEffectiveAspectRatio,
                                                    minSize = minSize
                                                )
                                                localCropBoundsState.value = newBounds
                                                onStagedCropBoundsChange(newBounds)
                                            } else if (zoomScale > 1.05f) {
                                                change.consume()
                                                val maxPanX = ((fittedWPx * (zoomScale - 1f)) / 2f).coerceAtLeast(0f)
                                                val maxPanY = ((fittedHPx * (zoomScale - 1f)) / 2f).coerceAtLeast(0f)
                                                panOffset = Offset(
                                                    x = (panOffset.x + dragAmount.x).coerceIn(-maxPanX, maxPanX),
                                                    y = (panOffset.y + dragAmount.y).coerceIn(-maxPanY, maxPanY)
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner Hardware-Accelerated Scaled & Panned Content Container
                        Box(
                            modifier = Modifier
                                .size(fittedW + touchMargin * 2, fittedH + touchMargin * 2)
                                .graphicsLayer {
                                    scaleX = zoomScale
                                    scaleY = zoomScale
                                    translationX = panOffset.x
                                    translationY = panOffset.y
                                    clip = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.size(fittedW, fittedH),
                                contentAlignment = Alignment.Center
                            ) {
                                val effectivePerspectiveQuad = if (isCropMode && cropPanelMode == CropPanelMode.PERSPECTIVE) {
                                    DocumentQuad.DEFAULT
                                } else {
                                    page.perspectiveQuad
                                }
                                val imageRequest = remember(context, page.uri, page.rotationDegrees, effectiveCropBounds, effectivePerspectiveQuad) {
                                    ImageRequest.Builder(context)
                                        .data(page.uri)
                                        .size(1080, 1920)
                                        .precision(Precision.INEXACT)
                                        .allowRgb565(true)
                                        .transformations(
                                            PagePreviewTransformation(
                                                rotationDegrees = page.rotationDegrees,
                                                cropBounds = effectiveCropBounds,
                                                perspectiveQuad = effectivePerspectiveQuad
                                            )
                                        )
                                        .crossfade(false)
                                        .build()
                                }
                                AsyncImage(
                                    model = imageRequest,
                                    contentDescription = page.displayName ?: "Page ${pageIndex + 1}",
                                    contentScale = ContentScale.Fit,
                                    colorFilter = colorFilter,
                                    onSuccess = { success ->
                                        val d = success.result.drawable
                                        if (d.intrinsicWidth > 0 && d.intrinsicHeight > 0) {
                                            imageAspectRatio = d.intrinsicWidth.toFloat() / d.intrinsicHeight.toFloat()
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Annotations Overlay (committed annotations + live drawing preview)
                                EditorAnnotationCanvas(
                                    annotations = page.annotations,
                                    activeAnnotation = if (isAnnotateMode && isCurrentFocusedPage) liveInProgressAnnotation else null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Interactive Crop or Perspective Overlay Canvas when in Crop mode for current page
                            if (isCropMode && isCurrentFocusedPage) {
                                if (cropPanelMode == CropPanelMode.PERSPECTIVE) {
                                    EditorPerspectiveOverlay(
                                        documentQuad = stagedPerspectiveQuad,
                                        onQuadChange = onStagedPerspectiveQuadChange,
                                        onQuadCommit = onStagedPerspectiveQuadChange,
                                        touchMargin = touchMargin,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    EditorCropOverlayCanvas(
                                        cropBounds = localCropBoundsState.value,
                                        activeHandle = activeCropHandle,
                                        touchMargin = touchMargin,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    EditorCanvasBadges(
                        page = page,
                        pageIndex = pageIndex,
                        totalPages = pages.size,
                        isCropMode = isCropMode,
                        isBadgeVisible = isBadgeVisible,
                        zoomScale = zoomScale,
                        onResetZoom = { animateZoom(1f, Offset.Zero) }
                    )
                }
            }
        }

        // Floating Lifted Card Overlay during reorder animation
        if (isReordering && movingPage != null) {
            EditorReorderLiftedCard(
                movingPage = movingPage,
                movingTargetIndex = movingTargetIndex,
                liftProgress = liftProgress
            )
        }

        // Floating Top Markup Action Bar (Undo, Redo, Clear All)
        AnimatedVisibility(
            visible = isAnnotateMode,
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 2 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(180)) { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(25f)
        ) {
            EditorMarkupActionBar(
                canUndo = canUndoAnnotation,
                canRedo = canRedoAnnotation,
                hasAnnotations = canUndoAnnotation,
                onUndo = onUndoAnnotation,
                onRedo = onRedoAnnotation,
                onClearAll = onClearAnnotations,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
