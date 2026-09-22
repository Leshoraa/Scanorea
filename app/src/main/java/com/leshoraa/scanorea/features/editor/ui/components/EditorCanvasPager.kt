package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImageCropBounds
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage
import kotlinx.coroutines.delay

/**
 * Image pager viewport displaying pages with rotation, cropping, filter indicators,
 * interactive crop overlays, and smooth reordering animations.
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isReordering && !isCropMode
        ) { pageIndex ->
            val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
            val colorMatrix = page.filter.createColorMatrix(page.contrast, page.brightness)
            val colorFilter = colorMatrix?.let { ColorFilter.colorMatrix(ColorMatrix(it.array)) }
            val isBeingMoved = isReordering && page.id == movingPage?.id
            val isCurrentFocusedPage = pageIndex == pagerState.currentPage

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
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isCropMode) isBadgeVisible = !isBadgeVisible
                        },
                    contentAlignment = Alignment.Center
                ) {
                    var imageAspectRatio by remember(page.id, page.rotationDegrees) { mutableFloatStateOf(0f) }

                    // Determine transformation based on active mode
                    val effectiveCropBounds = if (isCropMode && isCurrentFocusedPage) {
                        ImageCropBounds.DEFAULT
                    } else {
                        page.cropBounds
                    }

                    val imageModifier = if (imageAspectRatio > 0f) {
                        val containerAspect = maxWidth / maxHeight
                        val (fittedW, fittedH) = if (imageAspectRatio > containerAspect) {
                            maxWidth to (maxWidth / imageAspectRatio)
                        } else {
                            (maxHeight * imageAspectRatio) to maxHeight
                        }
                        Modifier.size(fittedW, fittedH)
                    } else {
                        Modifier.fillMaxSize()
                    }

                    Box(
                        modifier = imageModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(page.uri)
                                .transformations(
                                    PagePreviewTransformation(
                                        rotationDegrees = page.rotationDegrees,
                                        cropBounds = effectiveCropBounds
                                    )
                                )
                                .crossfade(true)
                                .build(),
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

                        // Interactive Crop Overlay when in Crop mode for current page
                        if (isCropMode && isCurrentFocusedPage) {
                            EditorCropOverlay(
                                cropBounds = page.cropBounds,
                                onCropChange = { newBounds ->
                                    onCropChange(page.id, newBounds)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
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
