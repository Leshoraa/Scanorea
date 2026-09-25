package com.leshoraa.scanorea.features.pdfviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.features.pdfviewer.domain.PdfScrollCalculator
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Interactive fast-scroller and thumb controller for the PDF document viewer.
 * Supports direct track dragging and real-time page detection based on thumb intersection.
 */
@Composable
fun PdfFastScroller(
    listState: LazyListState,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 1) return

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val thumbHeightDp = 38.dp
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
    val verticalPaddingDp = 16.dp
    val verticalPaddingPx = with(density) { verticalPaddingDp.toPx() }
    val spacingPx = with(density) { 8.dp.toPx() }

    var isDragging by remember { mutableStateOf(false) }
    var dragY by remember { mutableFloatStateOf(verticalPaddingPx) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(64.dp)
    ) {
        val containerHeightPx = constraints.maxHeight.toFloat()
        val minY = verticalPaddingPx
        val maxY = (containerHeightPx - thumbHeightPx - verticalPaddingPx).coerceAtLeast(minY)
        val travelRange = (maxY - minY).coerceAtLeast(1f)

        // Calculate progress from LazyListState when not dragging
        val scrollProgress by remember(listState, totalPages) {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isEmpty()) return@derivedStateOf 0f

                val itemBounds = visibleItems.map {
                    PdfScrollCalculator.ItemBounds(
                        index = it.index,
                        top = it.offset,
                        bottom = it.offset + it.size
                    )
                }

                PdfScrollCalculator.calculateScrollProgress(
                    firstVisibleIndex = listState.firstVisibleItemIndex,
                    firstVisibleOffset = listState.firstVisibleItemScrollOffset,
                    totalPages = totalPages,
                    visibleItems = itemBounds,
                    viewportStart = layoutInfo.viewportStartOffset,
                    viewportEnd = layoutInfo.viewportEndOffset,
                    spacingPx = spacingPx
                )
            }
        }

        val targetThumbY = if (isDragging) {
            dragY
        } else {
            minY + scrollProgress * travelRange
        }

        val thumbCenterY = targetThumbY + thumbHeightPx / 2f

        // Active page calculation: which paper is currently intersecting the scrollbar thumb's vertical center
        val activePage by remember(listState, totalPages, thumbCenterY) {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isEmpty()) return@derivedStateOf 1

                val itemBounds = visibleItems.map {
                    PdfScrollCalculator.ItemBounds(
                        index = it.index,
                        top = it.offset,
                        bottom = it.offset + it.size
                    )
                }

                PdfScrollCalculator.findPageAtThumbCenter(
                    thumbCenterY = thumbCenterY,
                    visibleItems = itemBounds,
                    totalPages = totalPages,
                    gapPx = spacingPx.toInt()
                )
            }
        }

        fun scrollToFraction(fraction: Float) {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val avgItemHeight = if (visibleItems.isNotEmpty()) {
                visibleItems.sumOf { it.size }.toFloat() / visibleItems.size
            } else {
                1000f
            }
            val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()

            val (targetIndex, targetOffset) = PdfScrollCalculator.calculateScrollTarget(
                fraction = fraction,
                totalPages = totalPages,
                avgItemHeight = avgItemHeight,
                spacingPx = spacingPx,
                viewportHeight = viewportHeight
            )

            coroutineScope.launch {
                listState.scrollToItem(targetIndex, targetOffset)
            }
        }

        // Entire right track area captures drag and touch gestures
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(64.dp)
                .systemGestureExclusion()
                .pointerInput(totalPages, travelRange, minY, maxY) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isDragging = true
                        dragY = (down.position.y - thumbHeightPx / 2f).coerceIn(minY, maxY)
                        val fraction = ((dragY - minY) / travelRange).coerceIn(0f, 1f)
                        scrollToFraction(fraction)

                        while (true) {
                            val event = awaitPointerEvent()
                            val dragChange = event.changes.firstOrNull { it.id == down.id }
                            if (dragChange == null || !dragChange.pressed) {
                                isDragging = false
                                break
                            }
                            dragY = (dragChange.position.y - thumbHeightPx / 2f).coerceIn(minY, maxY)
                            val currentFraction = ((dragY - minY) / travelRange).coerceIn(0f, 1f)
                            scrollToFraction(currentFraction)
                            dragChange.consume()
                        }
                    }
                }
        ) {
            // Subtle track bar line
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp, top = verticalPaddingDp, bottom = verticalPaddingDp)
                    .fillMaxHeight()
                    .width(3.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDragging) 0.25f else 0.12f),
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )

            // Draggable Thumb Pill
            Surface(
                color = if (isDragging) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.90f)
                },
                contentColor = if (isDragging) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.inverseOnSurface
                },
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    bottomStart = 18.dp,
                    topEnd = 6.dp,
                    bottomEnd = 6.dp
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(x = 0, y = targetThumbY.roundToInt()) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 7.dp, bottom = 7.dp)
                ) {
                    // Tactile grip indicator
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(width = 3.dp, height = 12.dp)
                            .background(
                                color = if (isDragging) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.5f)
                                },
                                shape = RoundedCornerShape(1.5.dp)
                            )
                    )

                    Text(
                        text = "$activePage / $totalPages",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
