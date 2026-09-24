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
import com.leshoraa.scanorea.features.editor.domain.model.AnnotationColors
import com.leshoraa.scanorea.features.editor.domain.model.AnnotationTool
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
    cropAspectRatio: CropAspectRatio = CropAspectRatio.FREE,
    onCropChange: (pageId: String, bounds: ImageCropBounds) -> Unit,
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
                    val localCropBoundsState = remember(page.id) { mutableStateOf(page.cropBounds) }
                    var activeCropHandle by remember { mutableStateOf(DragHandle.NONE) }
                    val currentCropAspectRatio by rememberUpdatedState(cropAspectRatio)
                    val currentEffectiveAspectRatio by rememberUpdatedState(page.effectiveAspectRatio)

                    LaunchedEffect(page.cropBounds) {
                        if (activeCropHandle == DragHandle.NONE && localCropBoundsState.value != page.cropBounds) {
                            localCropBoundsState.value = page.cropBounds
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

                                    if (currentIsAnnotateMode) {
                                        val isInsideImage = startPos.x in (imgScreenLeft - 24f)..(imgScreenLeft + imgScreenW + 24f) &&
                                                startPos.y in (imgScreenTop - 24f)..(imgScreenTop + imgScreenH + 24f)
                                        if (isInsideImage) {
                                            isAnnotating = true
                                            startNorm = toNorm(startPos)
                                            activePoints = mutableListOf(startNorm)

                                            activeAnno = when (currentAnnotationTool) {
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

                                    if (isCropMode) {
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
                                                if (localCropBoundsState.value != page.cropBounds) {
                                                    currentOnCropChange(page.id, localCropBoundsState.value)
                                                }
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

                                                val prev = localCropBoundsState.value

                                                if (activeCropHandle == DragHandle.BODY) {
                                                    val boxWidth = prev.right - prev.left
                                                    val boxHeight = prev.bottom - prev.top
                                                    val newL = (prev.left + deltaX).coerceIn(0f, 1f - boxWidth)
                                                    val newT = (prev.top + deltaY).coerceIn(0f, 1f - boxHeight)
                                                    val newR = newL + boxWidth
                                                    val newB = newT + boxHeight
                                                    localCropBoundsState.value = ImageCropBounds(newL, newT, newR, newB)
                                                } else if (currentCropAspectRatio == CropAspectRatio.FREE) {
                                                    var newL = prev.left
                                                    var newT = prev.top
                                                    var newR = prev.right
                                                    var newB = prev.bottom

                                                    when (activeCropHandle) {
                                                        DragHandle.TOP_LEFT -> {
                                                            newL = (prev.left + deltaX).coerceIn(0f, prev.right - minSize)
                                                            newT = (prev.top + deltaY).coerceIn(0f, prev.bottom - minSize)
                                                        }
                                                        DragHandle.TOP_RIGHT -> {
                                                            newR = (prev.right + deltaX).coerceIn(prev.left + minSize, 1f)
                                                            newT = (prev.top + deltaY).coerceIn(0f, prev.bottom - minSize)
                                                        }
                                                        DragHandle.BOTTOM_LEFT -> {
                                                            newL = (prev.left + deltaX).coerceIn(0f, prev.right - minSize)
                                                            newB = (prev.bottom + deltaY).coerceIn(prev.top + minSize, 1f)
                                                        }
                                                        DragHandle.BOTTOM_RIGHT -> {
                                                            newR = (prev.right + deltaX).coerceIn(prev.left + minSize, 1f)
                                                            newB = (prev.bottom + deltaY).coerceIn(prev.top + minSize, 1f)
                                                        }
                                                        DragHandle.TOP_EDGE -> {
                                                            newT = (prev.top + deltaY).coerceIn(0f, prev.bottom - minSize)
                                                        }
                                                        DragHandle.BOTTOM_EDGE -> {
                                                            newB = (prev.bottom + deltaY).coerceIn(prev.top + minSize, 1f)
                                                        }
                                                        DragHandle.LEFT_EDGE -> {
                                                            newL = (prev.left + deltaX).coerceIn(0f, prev.right - minSize)
                                                        }
                                                        DragHandle.RIGHT_EDGE -> {
                                                            newR = (prev.right + deltaX).coerceIn(prev.left + minSize, 1f)
                                                        }
                                                        DragHandle.BODY, DragHandle.NONE -> {}
                                                    }
                                                    if (newL < newR && newT < newB) {
                                                        localCropBoundsState.value = ImageCropBounds.ofClamped(newL, newT, newR, newB, minSize)
                                                    }
                                                } else {
                                                    val targetPhysicalRatio: Float = when (currentCropAspectRatio) {
                                                        CropAspectRatio.FREE -> 1.0f
                                                        CropAspectRatio.ORIGINAL -> currentEffectiveAspectRatio
                                                        CropAspectRatio.A4 -> CropAspectRatio.A4.ratio ?: (1f / 1.4142f)
                                                        CropAspectRatio.SQUARE -> 1.0f
                                                    }
                                                    val normRatio = (targetPhysicalRatio / currentEffectiveAspectRatio).coerceIn(0.01f, 100f)
                                                    val prevW = prev.right - prev.left
                                                    val prevH = prev.bottom - prev.top
                                                    val minW = if (normRatio >= 1f) (minSize * normRatio).coerceIn(minSize, 0.9f) else minSize
                                                    val minH = if (normRatio >= 1f) minSize else (minSize / normRatio).coerceIn(minSize, 0.9f)

                                                    var newL = prev.left
                                                    var newT = prev.top
                                                    var newR = prev.right
                                                    var newB = prev.bottom

                                                    when (activeCropHandle) {
                                                        DragHandle.BOTTOM_RIGHT -> {
                                                            val maxW = (1f - prev.left).coerceAtLeast(minW)
                                                            val maxH = (1f - prev.top).coerceAtLeast(minH)
                                                            val limitW = if (maxW / maxH > normRatio) maxH * normRatio else maxW
                                                            val limitH = limitW / normRatio

                                                            val reqW = prevW + deltaX
                                                            val reqH = prevH + deltaY
                                                            var w = if (kotlin.math.abs(deltaX) >= kotlin.math.abs(deltaY * normRatio)) {
                                                                reqW.coerceIn(minW, limitW)
                                                            } else {
                                                                (reqH * normRatio).coerceIn(minW, limitW)
                                                            }
                                                            var h = w / normRatio
                                                            if (h > limitH) {
                                                                h = limitH
                                                                w = h * normRatio
                                                            }
                                                            newR = newL + w
                                                            newB = newT + h
                                                        }
                                                        DragHandle.TOP_LEFT -> {
                                                            val maxW = prev.right.coerceAtLeast(minW)
                                                            val maxH = prev.bottom.coerceAtLeast(minH)
                                                            val limitW = if (maxW / maxH > normRatio) maxH * normRatio else maxW
                                                            val limitH = limitW / normRatio

                                                            val reqW = prevW - deltaX
                                                            val reqH = prevH - deltaY
                                                            var w = if (kotlin.math.abs(deltaX) >= kotlin.math.abs(deltaY * normRatio)) {
                                                                reqW.coerceIn(minW, limitW)
                                                            } else {
                                                                (reqH * normRatio).coerceIn(minW, limitW)
                                                            }
                                                            var h = w / normRatio
                                                            if (h > limitH) {
                                                                h = limitH
                                                                w = h * normRatio
                                                            }
                                                            newL = newR - w
                                                            newT = newB - h
                                                        }
                                                        DragHandle.TOP_RIGHT -> {
                                                            val maxW = (1f - prev.left).coerceAtLeast(minW)
                                                            val maxH = prev.bottom.coerceAtLeast(minH)
                                                            val limitW = if (maxW / maxH > normRatio) maxH * normRatio else maxW
                                                            val limitH = limitW / normRatio

                                                            val reqW = prevW + deltaX
                                                            val reqH = prevH - deltaY
                                                            var w = if (kotlin.math.abs(deltaX) >= kotlin.math.abs(deltaY * normRatio)) {
                                                                reqW.coerceIn(minW, limitW)
                                                            } else {
                                                                (reqH * normRatio).coerceIn(minW, limitW)
                                                            }
                                                            var h = w / normRatio
                                                            if (h > limitH) {
                                                                h = limitH
                                                                w = h * normRatio
                                                            }
                                                            newR = newL + w
                                                            newT = newB - h
                                                        }
                                                        DragHandle.BOTTOM_LEFT -> {
                                                            val maxW = prev.right.coerceAtLeast(minW)
                                                            val maxH = (1f - prev.top).coerceAtLeast(minH)
                                                            val limitW = if (maxW / maxH > normRatio) maxH * normRatio else maxW
                                                            val limitH = limitW / normRatio

                                                            val reqW = prevW - deltaX
                                                            val reqH = prevH + deltaY
                                                            var w = if (kotlin.math.abs(deltaX) >= kotlin.math.abs(deltaY * normRatio)) {
                                                                reqW.coerceIn(minW, limitW)
                                                            } else {
                                                                (reqH * normRatio).coerceIn(minW, limitW)
                                                            }
                                                            var h = w / normRatio
                                                            if (h > limitH) {
                                                                h = limitH
                                                                w = h * normRatio
                                                            }
                                                            newL = newR - w
                                                            newB = newT + h
                                                        }
                                                        DragHandle.LEFT_EDGE, DragHandle.RIGHT_EDGE -> {
                                                            val maxW = (if (normRatio <= 1f) normRatio else 1f).coerceAtLeast(minW)
                                                            val reqW = if (activeCropHandle == DragHandle.RIGHT_EDGE) prevW + deltaX else prevW - deltaX
                                                            val w = reqW.coerceIn(minW, maxW)
                                                            val h = w / normRatio
                                                            val centerH = (prev.top + prev.bottom) / 2f
                                                            var topCandidate = centerH - h / 2f
                                                            var bottomCandidate = centerH + h / 2f
                                                            if (topCandidate < 0f) {
                                                                bottomCandidate += (0f - topCandidate)
                                                                topCandidate = 0f
                                                            }
                                                            if (bottomCandidate > 1f) {
                                                                topCandidate -= (bottomCandidate - 1f)
                                                                bottomCandidate = 1f
                                                            }
                                                            newT = topCandidate.coerceIn(0f, 1f - h)
                                                            newB = (newT + h).coerceIn(newT + minH, 1f)
                                                            if (activeCropHandle == DragHandle.RIGHT_EDGE) {
                                                                newR = (prev.left + w).coerceIn(prev.left + minW, 1f)
                                                            } else {
                                                                newL = (prev.right - w).coerceIn(0f, prev.right - minW)
                                                            }
                                                        }
                                                        DragHandle.TOP_EDGE, DragHandle.BOTTOM_EDGE -> {
                                                            val maxH = (if (normRatio >= 1f) 1f / normRatio else 1f).coerceAtLeast(minH)
                                                            val reqH = if (activeCropHandle == DragHandle.BOTTOM_EDGE) prevH + deltaY else prevH - deltaY
                                                            val h = reqH.coerceIn(minH, maxH)
                                                            val w = h * normRatio
                                                            val centerW = (prev.left + prev.right) / 2f
                                                            var leftCandidate = centerW - w / 2f
                                                            var rightCandidate = centerW + w / 2f
                                                            if (leftCandidate < 0f) {
                                                                rightCandidate += (0f - leftCandidate)
                                                                leftCandidate = 0f
                                                            }
                                                            if (rightCandidate > 1f) {
                                                                leftCandidate -= (rightCandidate - 1f)
                                                                rightCandidate = 1f
                                                            }
                                                            newL = leftCandidate.coerceIn(0f, 1f - w)
                                                            newR = (newL + w).coerceIn(newL + minW, 1f)
                                                            if (activeCropHandle == DragHandle.BOTTOM_EDGE) {
                                                                newB = (prev.top + h).coerceIn(prev.top + minH, 1f)
                                                            } else {
                                                                newT = (prev.bottom - h).coerceIn(0f, prev.bottom - minH)
                                                            }
                                                        }
                                                        DragHandle.BODY, DragHandle.NONE -> {}
                                                    }
                                                    if (newL < newR && newT < newB) {
                                                        localCropBoundsState.value = ImageCropBounds(newL, newT, newR, newB)
                                                    }
                                                }
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
                                val imageRequest = remember(context, page.uri, page.rotationDegrees, effectiveCropBounds) {
                                    ImageRequest.Builder(context)
                                        .data(page.uri)
                                        .size(1080, 1920)
                                        .precision(Precision.INEXACT)
                                        .allowRgb565(true)
                                        .transformations(
                                            PagePreviewTransformation(
                                                rotationDegrees = page.rotationDegrees,
                                                cropBounds = effectiveCropBounds
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

                            // Interactive Crop Overlay Canvas when in Crop mode for current page
                            if (isCropMode && isCurrentFocusedPage) {
                                EditorCropOverlayCanvas(
                                    cropBounds = localCropBoundsState.value,
                                    activeHandle = activeCropHandle,
                                    touchMargin = touchMargin,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Top-left Transient Filter Badge (hidden during crop mode)
                    if (!isCropMode) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isBadgeVisible,
                            enter = fadeIn(animationSpec = tween(250)),
                            exit = fadeOut(animationSpec = tween(350)),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    text = page.filter.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Floating Zoom Reset Badge / Pill
                    androidx.compose.animation.AnimatedVisibility(
                        visible = zoomScale > 1.05f,
                        enter = fadeIn(tween(200)) + scaleIn(tween(200)),
                        exit = fadeOut(tween(200)) + scaleOut(tween(200)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = if (!isCropMode) 88.dp else 12.dp)
                    ) {
                        Surface(
                            onClick = {
                                animateZoom(1f, Offset.Zero)
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xDD202020),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", zoomScale)}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Reset Zoom",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    // Top-right Page Indicator Badge
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "${pageIndex + 1} / ${pages.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Floating Lifted Card Overlay during reorder animation
        if (isReordering && movingPage != null) {
            val liftedPage = movingPage
            val colorMatrix = liftedPage.filter.createColorMatrix(liftedPage.contrast, liftedPage.brightness)
            val colorFilter = colorMatrix?.let { ColorFilter.colorMatrix(ColorMatrix(it.array)) }

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
                    .graphicsLayer {
                        translationY = -46.dp.toPx() * liftProgress
                        scaleX = 1f + 0.05f * liftProgress
                        scaleY = 1f + 0.05f * liftProgress
                    }
                    .padding(4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(liftedPage.uri)
                            .transformations(
                                PagePreviewTransformation(
                                    rotationDegrees = liftedPage.rotationDegrees,
                                    cropBounds = liftedPage.cropBounds
                                )
                            )
                            .crossfade(false)
                            .build(),
                        contentDescription = liftedPage.displayName,
                        contentScale = ContentScale.Fit,
                        colorFilter = colorFilter,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Moving to Page ${movingTargetIndex + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
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
