package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImageCropBounds
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.hypot

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
    onCropChange: (pageId: String, bounds: ImageCropBounds) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeZoomScale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(pagerState.currentPage, isCropMode) {
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
            userScrollEnabled = !isReordering && !isCropMode && (activeZoomScale <= 1.05f)
        ) { pageIndex ->
            val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
            val colorMatrix = page.filter.createColorMatrix(page.contrast, page.brightness)
            val colorFilter = colorMatrix?.let { ColorFilter.colorMatrix(ColorMatrix(it.array)) }
            val isBeingMoved = isReordering && page.id == movingPage?.id
            val isCurrentFocusedPage = pageIndex == pagerState.currentPage

            var zoomScale by remember(page.id, isCropMode) { mutableFloatStateOf(1f) }
            var panOffset by remember(page.id, isCropMode) { mutableStateOf(Offset.Zero) }

            LaunchedEffect(isCurrentFocusedPage) {
                if (!isCurrentFocusedPage) {
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    var localCropBounds by remember(page.id, page.cropBounds) { mutableStateOf(page.cropBounds) }
                    var activeCropHandle by remember { mutableStateOf(DragHandle.NONE) }

                    LaunchedEffect(page.cropBounds) {
                        if (activeCropHandle == DragHandle.NONE && localCropBounds != page.cropBounds) {
                            localCropBounds = page.cropBounds
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
                            .pointerInput(page.id, isCropMode, isCurrentFocusedPage, fittedWPx, fittedHPx) {
                                if (!isCurrentFocusedPage) return@pointerInput

                                val cornerHitRadius = 34.dp.toPx()
                                val edgeHitRadius = 42.dp.toPx()
                                val edgePad = 16.dp.toPx()

                                var lastTapTime = 0L
                                var lastTapPos = Offset.Zero

                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val startPos = down.position
                                    val startTime = System.currentTimeMillis()

                                    val viewW = size.width.toFloat()
                                    val viewH = size.height.toFloat()
                                    val centerX = viewW / 2f
                                    val centerY = viewH / 2f

                                    var handle = DragHandle.NONE

                                    if (isCropMode) {
                                        val bounds = localCropBounds
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
                                            val duration = System.currentTimeMillis() - startTime
                                            if (totalDragDistance < 15f && duration < 300L) {
                                                val now = System.currentTimeMillis()
                                                if (now - lastTapTime < 350L && hypot(startPos.x - lastTapPos.x, startPos.y - lastTapPos.y) < 60.dp.toPx()) {
                                                    if (zoomScale > 1.05f) {
                                                        zoomScale = 1f
                                                        activeZoomScale = 1f
                                                        panOffset = Offset.Zero
                                                    } else {
                                                        val targetScale = 2.5f
                                                        zoomScale = targetScale
                                                        activeZoomScale = targetScale
                                                        val maxPanX = ((fittedWPx * (targetScale - 1f)) / 2f).coerceAtLeast(0f)
                                                        val maxPanY = ((fittedHPx * (targetScale - 1f)) / 2f).coerceAtLeast(0f)
                                                        val cRel = startPos - Offset(centerX, centerY)
                                                        panOffset = Offset(
                                                            x = (-cRel.x * 1.5f).coerceIn(-maxPanX, maxPanX),
                                                            y = (-cRel.y * 1.5f).coerceIn(-maxPanY, maxPanY)
                                                        )
                                                    }
                                                    lastTapTime = 0L
                                                } else {
                                                    lastTapTime = now
                                                    lastTapPos = startPos
                                                    if (!isCropMode) {
                                                        isBadgeVisible = !isBadgeVisible
                                                    }
                                                }
                                            }

                                            if (activeCropHandle != DragHandle.NONE) {
                                                if (localCropBounds != page.cropBounds) {
                                                    onCropChange(page.id, localCropBounds)
                                                }
                                            }
                                            activeCropHandle = DragHandle.NONE
                                            break
                                        }

                                        if (pressed.size >= 2) {
                                            isPinch = true
                                            activeCropHandle = DragHandle.NONE

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

                                            if (activeCropHandle != DragHandle.NONE) {
                                                change.consume()
                                                val deltaX = dragAmount.x / (fittedWPx * zoomScale)
                                                val deltaY = dragAmount.y / (fittedHPx * zoomScale)
                                                val minSize = 0.08f

                                                val prev = localCropBounds
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
                                                    DragHandle.NONE -> {}
                                                }
                                                if (newL < newR && newT < newB) {
                                                    localCropBounds = ImageCropBounds.ofClamped(newL, newT, newR, newB, minSize)
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
                            }

                            // Interactive Crop Overlay Canvas when in Crop mode for current page
                            if (isCropMode && isCurrentFocusedPage) {
                                EditorCropOverlayCanvas(
                                    cropBounds = localCropBounds,
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
                                shadowElevation = 2.dp
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
                                zoomScale = 1f
                                activeZoomScale = 1f
                                panOffset = Offset.Zero
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xDD202020),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                            shadowElevation = 6.dp
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
                elevation = CardDefaults.cardElevation(defaultElevation = (6.dp + 18.dp * liftProgress))
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
                        shadowElevation = 4.dp,
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
    }
}
