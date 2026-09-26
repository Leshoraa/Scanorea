package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage

/**
 * Modal bottom sheet presenting a bird's-eye grid overview of all pages in the document.
 * Enables users to jump to any page, drag and drop to reorder sequence, delete individual pages, or rotate all pages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorPagesGridDialog(
    pages: List<ImagePage>,
    currentPageIndex: Int,
    onPageSelected: (Int) -> Unit,
    onMovePage: (sourceIndex: Int, targetIndex: Int) -> Unit,
    onRemovePage: (pageId: String) -> Unit,
    onRotateAllPages: () -> Unit,
    onAddPagesClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val gridState = rememberLazyGridState()

    var localPages by remember(pages) { mutableStateOf(pages) }

    var draggedKey by remember { mutableStateOf<String?>(null) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var draggedItemSize by remember { mutableStateOf(Size.Zero) }

    val spacingPx = with(density) { 10.dp.toPx() }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Document Pages (${localPages.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap to jump  •  Drag to reorder",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pages Grid Container with Floating Overlay Layer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((localPages.size.coerceAtLeast(3) * 60).coerceIn(240, 420).dp)
                ) {
                    itemsIndexed(localPages, key = { _, page -> page.id }) { index, page ->
                        val currentIndex = localPages.indexOfFirst { it.id == page.id }.takeIf { it != -1 } ?: index
                        val isDragging = draggedKey == page.id
                        val isCurrent = page.id == localPages.getOrNull(currentPageIndex)?.id
                        val colorFilter = remember(page.filter, page.contrast, page.brightness) {
                            val matrix = page.filter.createColorMatrix(page.contrast, page.brightness)
                            matrix?.let { ColorFilter.colorMatrix(ColorMatrix(it.array)) }
                        }
                        val imageRequest = remember(page.uri, page.rotationDegrees, page.cropBounds) {
                            ImageRequest.Builder(context)
                                .data(page.uri)
                                .size(240, 320)
                                .precision(Precision.INEXACT)
                                .allowRgb565(true)
                                .transformations(
                                    PagePreviewTransformation(
                                        rotationDegrees = page.rotationDegrees,
                                        cropBounds = page.cropBounds
                                    )
                                )
                                .crossfade(false)
                                .build()
                        }

                        // Dynamic slot measurements
                        val visibleItems = gridState.layoutInfo.visibleItemsInfo
                        val firstItem = visibleItems.firstOrNull()
                        val itemWidth = firstItem?.size?.width?.toFloat() ?: with(density) { 100.dp.toPx() }
                        val itemHeight = firstItem?.size?.height?.toFloat() ?: with(density) { 140.dp.toPx() }
                        val slotWidth = itemWidth + spacingPx
                        val slotHeight = itemHeight + spacingPx

                        // Calculate prospective visual slot for non-dragged items
                        val activeDraggedIndex = draggedIndex
                        val activeHoveredIndex = hoveredIndex

                        val targetSlot = when {
                            activeDraggedIndex == null || activeHoveredIndex == null || isDragging -> currentIndex
                            activeHoveredIndex < activeDraggedIndex -> {
                                if (currentIndex in activeHoveredIndex until activeDraggedIndex) currentIndex + 1 else currentIndex
                            }
                            activeHoveredIndex > activeDraggedIndex -> {
                                if (currentIndex in (activeDraggedIndex + 1)..activeHoveredIndex) currentIndex - 1 else currentIndex
                            }
                            else -> currentIndex
                        }

                        val deltaCol = (targetSlot % 3) - (currentIndex % 3)
                        val deltaRow = (targetSlot / 3) - (currentIndex / 3)
                        val shiftX = deltaCol * slotWidth
                        val shiftY = deltaRow * slotHeight

                        val animatedShiftX by animateFloatAsState(
                            targetValue = shiftX,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "shiftX_$currentIndex"
                        )
                        val animatedShiftY by animateFloatAsState(
                            targetValue = shiftY,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "shiftY_$currentIndex"
                        )

                        val displayPageNumber = when {
                            isDragging -> (hoveredIndex ?: currentIndex) + 1
                            else -> targetSlot + 1
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                                .alpha(if (isDragging) 0.30f else 1f)
                                .graphicsLayer {
                                    translationX = if (isDragging) 0f else animatedShiftX
                                    translationY = if (isDragging) 0f else animatedShiftY
                                }
                                .pointerInput(page.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val liveIndex = localPages.indexOfFirst { it.id == page.id }.takeIf { it != -1 } ?: currentIndex
                                            val itemInfo = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == page.id }
                                            if (itemInfo != null) {
                                                dragStartOffset = Offset(itemInfo.offset.x.toFloat(), itemInfo.offset.y.toFloat())
                                                draggedItemSize = Size(itemInfo.size.width.toFloat(), itemInfo.size.height.toFloat())
                                            }
                                            draggedKey = page.id
                                            draggedIndex = liveIndex
                                            hoveredIndex = liveIndex
                                            dragOffset = Offset.Zero
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount

                                            val anchor = gridState.layoutInfo.visibleItemsInfo.firstOrNull()
                                            val draggedItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggedKey }
                                            val liveIndex = localPages.indexOfFirst { it.id == page.id }.takeIf { it != -1 } ?: currentIndex

                                            if (draggedItem != null && anchor != null && localPages.isNotEmpty()) {
                                                val itemW = anchor.size.width.toFloat()
                                                val itemH = anchor.size.height.toFloat()
                                                val sWidth = itemW + spacingPx
                                                val sHeight = itemH + spacingPx

                                                val currentCenterX = draggedItem.offset.x + draggedItem.size.width / 2f + dragOffset.x
                                                val currentCenterY = draggedItem.offset.y + draggedItem.size.height / 2f + dragOffset.y

                                                val totalRows = (localPages.size + 2) / 3
                                                val isFarOutside = currentCenterY < (anchor.offset.y - itemH) ||
                                                        currentCenterY > (anchor.offset.y + totalRows * sHeight + itemH)

                                                val newHoveredIndex = if (isFarOutside) {
                                                    liveIndex
                                                } else {
                                                    localPages.indices.minByOrNull { i ->
                                                        val dCol = (i % 3) - (anchor.index % 3)
                                                        val dRow = (i / 3) - (anchor.index / 3)
                                                        val slotCx = anchor.offset.x + dCol * sWidth + itemW / 2f
                                                        val slotCy = anchor.offset.y + dRow * sHeight + itemH / 2f
                                                        val dx = currentCenterX - slotCx
                                                        val dy = currentCenterY - slotCy
                                                        dx * dx + dy * dy
                                                    } ?: liveIndex
                                                }

                                                if (newHoveredIndex != hoveredIndex) {
                                                    hoveredIndex = newHoveredIndex
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            val from = draggedIndex
                                            val to = hoveredIndex
                                            if (from != null && to != null && from != to && from in localPages.indices && to in localPages.indices) {
                                                val updated = localPages.toMutableList()
                                                val item = updated.removeAt(from)
                                                updated.add(to, item)
                                                localPages = updated
                                                onMovePage(from, to)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                            draggedKey = null
                                            draggedIndex = null
                                            hoveredIndex = null
                                            dragOffset = Offset.Zero
                                            dragStartOffset = Offset.Zero
                                            draggedItemSize = Size.Zero
                                        },
                                        onDragCancel = {
                                            draggedKey = null
                                            draggedIndex = null
                                            hoveredIndex = null
                                            dragOffset = Offset.Zero
                                            dragStartOffset = Offset.Zero
                                            draggedItemSize = Size.Zero
                                        }
                                    )
                                }
                                .clickable(enabled = draggedKey == null) {
                                    val selIndex = localPages.indexOfFirst { it.id == page.id }.takeIf { it != -1 } ?: currentIndex
                                    onPageSelected(selIndex)
                                    onDismissRequest()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            border = when {
                                isDragging -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                isCurrent -> BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
                                else -> null
                            }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Page Thumbnail Image
                                AsyncImage(
                                    model = imageRequest,
                                    contentDescription = page.displayName ?: "Page ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    colorFilter = colorFilter,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                )

                                // Page Number Badge (Top-Left)
                                Surface(
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.70f),
                                    shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 8.dp),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = "$displayPageNumber",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                // Delete Page Button (Top-Right)
                                if (localPages.size > 1 && draggedKey == null) {
                                    FilledTonalIconButton(
                                        onClick = { onRemovePage(page.id) },
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = Color.Black.copy(alpha = 0.65f),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Delete Page",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add Page Card Item
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                                .clickable {
                                    onAddPagesClick()
                                    onDismissRequest()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.AddPhotoAlternate,
                                            contentDescription = "Add page",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Add Page",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Unconstrained Floating Dragged Card (Immune to LazyVerticalGrid clipping)
                if (draggedKey != null) {
                    val draggedPage = localPages.firstOrNull { it.id == draggedKey }
                    if (draggedPage != null && draggedItemSize.width > 0f) {
                        val draggedW = with(density) { draggedItemSize.width.toDp() }
                        val draggedH = with(density) { draggedItemSize.height.toDp() }
                        val floatingColorFilter = remember(draggedPage.filter, draggedPage.contrast, draggedPage.brightness) {
                            val matrix = draggedPage.filter.createColorMatrix(draggedPage.contrast, draggedPage.brightness)
                            matrix?.let { ColorFilter.colorMatrix(ColorMatrix(it.array)) }
                        }
                        val floatingImageRequest = remember(draggedPage.uri, draggedPage.rotationDegrees, draggedPage.cropBounds) {
                            ImageRequest.Builder(context)
                                .data(draggedPage.uri)
                                .size(240, 320)
                                .precision(Precision.INEXACT)
                                .allowRgb565(true)
                                .transformations(
                                    PagePreviewTransformation(
                                        rotationDegrees = draggedPage.rotationDegrees,
                                        cropBounds = draggedPage.cropBounds
                                    )
                                )
                                .crossfade(false)
                                .build()
                        }
                        val displayPageNumber = (hoveredIndex ?: draggedIndex ?: 0) + 1

                        Card(
                            modifier = Modifier
                                .size(draggedW, draggedH)
                                .graphicsLayer {
                                    translationX = dragStartOffset.x + dragOffset.x
                                    translationY = dragStartOffset.y + dragOffset.y
                                    scaleX = 1.08f
                                    scaleY = 1.08f
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = floatingImageRequest,
                                    contentDescription = draggedPage.displayName ?: "Dragged Page",
                                    contentScale = ContentScale.Crop,
                                    colorFilter = floatingColorFilter,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                )

                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 8.dp),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = "$displayPageNumber",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onRotateAllPages,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.RotateRight,
                        contentDescription = "Rotate All",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rotate All (90°)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
