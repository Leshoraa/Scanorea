package com.leshoraa.scanorea.features.onboarding.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.core.filter.ImageFilterType
import com.leshoraa.scanorea.features.editor.domain.model.DocumentQuad
import com.leshoraa.scanorea.features.editor.domain.model.EditorCategory
import com.leshoraa.scanorea.features.editor.ui.components.CropPanelMode
import com.leshoraa.scanorea.features.editor.ui.components.EditorCanvasPager
import com.leshoraa.scanorea.features.editor.ui.components.EditorCategoryTabBar
import com.leshoraa.scanorea.features.editor.ui.components.EditorCropPanel
import com.leshoraa.scanorea.features.editor.ui.components.EditorFiltersPanel
import com.leshoraa.scanorea.features.home.ui.components.HomeHeroBanner
import com.leshoraa.scanorea.features.home.ui.components.QuickToolsSection
import com.leshoraa.scanorea.features.imagestopdf.domain.model.CompressionProfile
import com.leshoraa.scanorea.features.imagestopdf.domain.model.CropAspectRatio
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfConversionOptions
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageOrientation
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageSize
import com.leshoraa.scanorea.features.imagestopdf.ui.components.ConversionOptionsContent
import com.leshoraa.scanorea.features.recentpdfs.domain.model.RecentPdf
import com.leshoraa.scanorea.features.recentpdfs.ui.ResultsScreen
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.min

/**
 * First-launch guide previews built from the same composables the app uses in
 * production. The live UI is composed at a fixed virtual phone size and scaled
 * into the guide card, so the tour always mirrors the real screens instead of
 * maintaining a parallel set of hand-drawn mockups.
 */

@Composable
internal fun HomePreview(modifier: Modifier = Modifier) {
    GuidePreviewFrame(modifier = modifier) {
        ScaledPreview(virtualWidth = 400.dp, virtualHeight = 460.dp) {
            Column(modifier = Modifier.fillMaxSize()) {
                HomeHeroBanner(onScanConvertClick = {})
                Spacer(modifier = Modifier.height(6.dp))
                QuickToolsSection(
                    onGalleryClick = {},
                    onCameraClick = {},
                    onSeeAllClick = {}
                )
            }
        }
    }
}

@Composable
internal fun PagesPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val demoUri = rememberDemoDocumentUri(context)
    val pages = remember(demoUri) {
        List(3) { index ->
            ImagePage(
                id = "guide_page_$index",
                uri = demoUri,
                displayName = "Page ${index + 1}",
                width = 1200,
                height = 1600,
                filter = ImageFilterType.BLACK_AND_WHITE
            )
        }
    }
    val pagerState = rememberPagerState(
        initialPage = pages.size,
        pageCount = { pages.size + 1 }
    )

    GuidePreviewFrame(modifier = modifier) {
        ScaledPreview(virtualWidth = 400.dp, virtualHeight = 390.dp) {
            Column(modifier = Modifier.fillMaxSize()) {
                EditorCanvasPager(
                    pages = pages,
                    pagerState = pagerState,
                    isReordering = false,
                    movingPage = null,
                    movingTargetIndex = -1,
                    liftProgress = 0f,
                    isCropMode = false,
                    modifier = Modifier.weight(1f)
                )
                EditorCategoryTabBar(
                    selectedCategory = EditorCategory.FILTERS,
                    onCategorySelected = {}
                )
            }
        }
    }
}

@Composable
internal fun FiltersPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val demoUri = rememberDemoDocumentUri(context)
    val page = remember(demoUri) {
        ImagePage(
            id = "guide_filter_page",
            uri = demoUri,
            displayName = "Receipt",
            width = 1200,
            height = 1600,
            filter = ImageFilterType.BLACK_AND_WHITE,
            contrast = 1.18f,
            brightness = 4f
        )
    }
    val pages = remember(page) { listOf(page) }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pages.size }
    )

    GuidePreviewFrame(modifier = modifier) {
        ScaledPreview(virtualWidth = 400.dp, virtualHeight = 400.dp) {
            Column(modifier = Modifier.fillMaxSize()) {
                EditorCanvasPager(
                    pages = pages,
                    pagerState = pagerState,
                    isReordering = false,
                    movingPage = null,
                    movingTargetIndex = -1,
                    liftProgress = 0f,
                    isCropMode = false,
                    modifier = Modifier.weight(1f)
                )
                EditorCategoryTabBar(
                    selectedCategory = EditorCategory.FILTERS,
                    onCategorySelected = {}
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EditorFiltersPanel(
                        selectedFilter = ImageFilterType.BLACK_AND_WHITE,
                        onFilterSelected = {},
                        onApplyToAll = {}
                    )
                }
            }
        }
    }
}

@Composable
internal fun PerspectivePreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val demoUri = rememberPerspectiveDocumentUri(context)
    val perspectiveQuad = remember {
        DocumentQuad(
            topLeftX = 0.14f,
            topLeftY = 0.10f,
            topRightX = 0.86f,
            topRightY = 0.06f,
            bottomRightX = 0.92f,
            bottomRightY = 0.90f,
            bottomLeftX = 0.08f,
            bottomLeftY = 0.84f
        )
    }
    val page = remember(demoUri, perspectiveQuad) {
        ImagePage(
            id = "guide_perspective_page",
            uri = demoUri,
            displayName = "Receipt",
            width = 1200,
            height = 1600,
            filter = ImageFilterType.ENHANCED
        )
    }
    val pages = remember(page) { listOf(page) }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pages.size }
    )

    GuidePreviewFrame(modifier = modifier) {
        ScaledPreview(virtualWidth = 400.dp, virtualHeight = 410.dp) {
            Column(modifier = Modifier.fillMaxSize()) {
                EditorCanvasPager(
                    pages = pages,
                    pagerState = pagerState,
                    isReordering = false,
                    movingPage = null,
                    movingTargetIndex = -1,
                    liftProgress = 0f,
                    isCropMode = true,
                    cropPanelMode = CropPanelMode.PERSPECTIVE,
                    stagedPerspectiveQuad = perspectiveQuad,
                    onStagedPerspectiveQuadChange = {},
                    modifier = Modifier.weight(1f)
                )
                EditorCategoryTabBar(
                    selectedCategory = EditorCategory.CROP,
                    onCategorySelected = {}
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EditorCropPanel(
                        page = page,
                        currentPageIndex = 0,
                        totalPages = 1,
                        isReordering = false,
                        cropPanelMode = CropPanelMode.PERSPECTIVE,
                        onCropPanelModeChange = {},
                        selectedRatio = CropAspectRatio.FREE,
                        onRatioSelected = {},
                        stagedCropBounds = page.cropBounds,
                        stagedPerspectiveQuad = perspectiveQuad,
                        onCropBoundsChange = {},
                        onRotatePage = {},
                        onRotatePageLeft = {},
                        onResetCrop = {},
                        onResetPerspective = {},
                        onApplyCrop = {},
                        onPreviousPage = {},
                        onNextPage = {},
                        onMovePage = { _, _ -> },
                        isAutoDetecting = false
                    )
                }
            }
        }
    }
}

@Composable
internal fun FoldersPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val projectProposal = rememberDemoPdf(context, "Project_proposal.pdf", "Project Proposal")
    val receipts = rememberDemoPdf(context, "Receipts.pdf", "Receipts")
    val recentPdfs = remember(projectProposal, receipts) {
        listOf(
            RecentPdf(
                file = projectProposal,
                name = "Project_proposal.pdf",
                sizeBytes = projectProposal.length(),
                lastModifiedMillis = System.currentTimeMillis() - 18 * 60 * 1000L,
                folders = listOf("Personal"),
                isFavorite = true
            ),
            RecentPdf(
                file = receipts,
                name = "Receipts.pdf",
                sizeBytes = receipts.length(),
                lastModifiedMillis = System.currentTimeMillis() - 3 * 60 * 60 * 1000L,
                folders = listOf("Work")
            )
        )
    }

    GuidePreviewFrame(modifier = modifier) {
        ScaledPreview(virtualWidth = 400.dp, virtualHeight = 440.dp) {
            ResultsScreen(
                recentPdfs = recentPdfs,
                categories = listOf("Study", "Work", "Personal"),
                pinnedFolders = listOf("Study", "Work", "Personal"),
                onPdfClick = {},
                onShareClick = {},
                onDeleteClick = {},
                onToggleFavorite = {},
                onAssignFolders = { _, _ -> },
                onAddCategory = {},
                onRenameCategory = { _, _ -> },
                onDeleteCategory = {},
                isLoading = false,
                isRefreshing = false,
                onRefresh = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExportPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val demoUri = rememberDemoDocumentUri(context)
    val pages = remember(demoUri) {
        List(3) { index ->
            ImagePage(
                id = "guide_export_$index",
                uri = demoUri,
                displayName = "Page ${index + 1}",
                width = 1200,
                height = 1600,
                filter = ImageFilterType.BLACK_AND_WHITE
            )
        }
    }
    val options = remember {
        PdfConversionOptions(
            fileName = "Document_scan",
            pageSize = PdfPageSize.A4,
            orientation = PdfPageOrientation.PORTRAIT,
            compressionProfile = CompressionProfile.AUTO_BALANCED
        )
    }

    GuidePreviewFrame(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                BottomSheetDefaults.DragHandle()
                ConversionOptionsContent(
                    options = options,
                    pageCount = pages.size,
                    presets = emptyList(),
                    destinationFolderDisplayName = "Documents/Scanorea",
                    onFileNameChange = {},
                    onPageSizeChange = {},
                    pages = pages,
                    onOrientationChange = {},
                    onCompressionProfileChange = {},
                    onPresetSelected = {},
                    onSaveNewPreset = { _, _ -> },
                    onDeletePreset = {},
                    onChangeFolderClick = {},
                    onConvertClick = {},
                    categories = listOf("Study", "Work", "Personal"),
                    selectedFolders = listOf("Study"),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    applyNavigationBarsPadding = false
                )
            }
        }
    }
}

/**
 * Transparent gesture layer that keeps the embedded live UI from reacting to
 * touches while still letting the guide carousel be swiped directly on top of
 * the preview.
 */
@Composable
internal fun PreviewGestureLayer(
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnSwipeNext by rememberUpdatedState(onSwipeNext)
    val currentOnSwipePrevious by rememberUpdatedState(onSwipePrevious)

    Box(
        modifier = modifier.pointerInput(Unit) {
            val swipeThreshold = 48.dp.toPx()

            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()

                var horizontalDrag = 0f
                var isHorizontal: Boolean? = null

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break

                    val delta = change.positionChange()
                    if (isHorizontal == null) {
                        if (abs(delta.x) > viewConfiguration.touchSlop ||
                            abs(delta.y) > viewConfiguration.touchSlop
                        ) {
                            isHorizontal = abs(delta.x) >= abs(delta.y)
                        }
                    }

                    if (isHorizontal == true) {
                        horizontalDrag += delta.x
                    }
                    change.consume()
                }

                if (isHorizontal == true) {
                    if (horizontalDrag <= -swipeThreshold) {
                        currentOnSwipeNext()
                    } else if (horizontalDrag >= swipeThreshold) {
                        currentOnSwipePrevious()
                    }
                }
            }
        }
    )
}

@Composable
private fun GuidePreviewFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            )
            .padding(10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp
        ) {
            Box(modifier = Modifier.fillMaxSize(), content = content)
        }
    }
}

@Composable
private fun ScaledPreview(
    virtualWidth: Dp,
    virtualHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // The guide preview is always 340dp tall with 10dp of frame padding on
        // each side. Using that known height avoids the scaled child inflating
        // the measurement constraints of this BoxWithConstraints.
        val scale = min(
            maxWidth / virtualWidth,
            320.dp / virtualHeight
        )
        val density = LocalDensity.current
        val virtualWidthPx = with(density) { virtualWidth.roundToPx() }
        val virtualHeightPx = with(density) { virtualHeight.roundToPx() }

        Layout(
            modifier = Modifier
                .size(virtualWidth * scale, virtualHeight * scale)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(18.dp)
                ),
            content = {
                Box(
                    modifier = Modifier
                        .requiredSize(virtualWidth, virtualHeight)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                ) {
                    content()
                }
            }
        ) { measurables, constraints ->
            // Measure the live UI at its real phone dimensions, then place it
            // at the top-left of the clipped preview and let graphicsLayer
            // shrink the drawing. Box alignment is intentionally not used here
            // because oversized children would otherwise be centred.
            val placeable = measurables.first().measure(
                Constraints.fixed(virtualWidthPx, virtualHeightPx)
            )
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.place(0, 0)
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Demo content used only inside the first-launch guide.                      */
/* ------------------------------------------------------------------------- */

@Composable
private fun rememberDemoDocumentUri(context: Context): Uri {
    return remember(context) {
        val file = File(demoDirectory(context), "guide_document.png")
        if (!file.exists() || file.length() == 0L) {
            writeDemoDocumentImage(file)
        }
        Uri.fromFile(file)
    }
}

@Composable
private fun rememberPerspectiveDocumentUri(context: Context): Uri {
    return remember(context) {
        val file = File(demoDirectory(context), "guide_perspective.png")
        if (!file.exists() || file.length() == 0L) {
            writePerspectiveDocumentImage(file)
        }
        Uri.fromFile(file)
    }
}

@Composable
private fun rememberDemoPdf(context: Context, fileName: String, title: String): File {
    return remember(context, fileName, title) {
        val file = File(demoDirectory(context), fileName)
        if (!file.exists() || file.length() == 0L) {
            writeDemoPdf(file, title)
        }
        file
    }
}

private fun demoDirectory(context: Context): File {
    return File(context.cacheDir, "onboarding_demo").apply { mkdirs() }
}

private fun writeDemoDocumentImage(file: File) {
    val width = 1200
    val height = 1600
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.rgb(247, 243, 234))

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = AndroidColor.rgb(32, 30, 28)
    paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    paint.textSize = 74f
    canvas.drawText("INVOICE", 120f, 220f, paint)

    paint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    paint.textSize = 34f
    paint.color = AndroidColor.rgb(96, 84, 70)
    canvas.drawText("Scanorea Studio  •  Local document", 120f, 286f, paint)

    paint.color = AndroidColor.rgb(42, 38, 34)
    paint.strokeWidth = 4f
    canvas.drawLine(120f, 360f, width - 120f, 360f, paint)

    paint.strokeWidth = 2f
    paint.color = AndroidColor.rgb(120, 108, 94)
    repeat(12) { index ->
        val y = 430f + index * 58f
        val lineEnd = width - 120f - (index % 3) * 80f
        canvas.drawLine(120f, y, lineEnd, y, paint)
    }

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f
    paint.color = AndroidColor.rgb(42, 38, 34)
    canvas.drawRoundRect(
        width - 440f,
        height - 270f,
        width - 120f,
        height - 120f,
        18f,
        18f,
        paint
    )

    paint.style = Paint.Style.FILL
    paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    paint.textSize = 38f
    canvas.drawText("TOTAL", width - 400f, height - 205f, paint)
    paint.textSize = 58f
    canvas.drawText("\$42.00", width - 340f, height - 148f, paint)

    FileOutputStream(file).use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    }
    bitmap.recycle()
}

private fun writePerspectiveDocumentImage(file: File) {
    val width = 1200
    val height = 1600
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.rgb(28, 30, 34))

    val quad = Path().apply {
        moveTo(width * 0.14f, height * 0.10f)
        lineTo(width * 0.86f, height * 0.06f)
        lineTo(width * 0.92f, height * 0.90f)
        lineTo(width * 0.08f, height * 0.84f)
        close()
    }

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(90, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.save()
    canvas.translate(18f, 24f)
    canvas.drawPath(quad, shadowPaint)
    canvas.restore()

    val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(248, 244, 236)
        style = Paint.Style.FILL
    }
    canvas.drawPath(quad, paperPaint)

    canvas.save()
    canvas.clipPath(quad)

    val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(50, 44, 38)
    }
    ink.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    ink.textSize = 66f
    canvas.drawText("RECEIPT", width * 0.22f, height * 0.20f, ink)

    ink.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    ink.textSize = 30f
    ink.color = AndroidColor.rgb(112, 96, 78)
    canvas.drawText("Scanorea  •  Paid", width * 0.22f, height * 0.25f, ink)

    ink.color = AndroidColor.rgb(60, 52, 44)
    ink.strokeWidth = 3f
    canvas.drawLine(width * 0.22f, height * 0.31f, width * 0.72f, height * 0.30f, ink)

    ink.strokeWidth = 2f
    ink.color = AndroidColor.rgb(118, 102, 84)
    repeat(9) { index ->
        val y = height * (0.36f + index * 0.045f)
        canvas.drawLine(
            width * 0.22f,
            y,
            width * (0.70f + (index % 2) * 0.05f),
            y - 8f,
            ink
        )
    }

    ink.style = Paint.Style.FILL
    ink.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    ink.textSize = 54f
    ink.color = AndroidColor.rgb(50, 44, 38)
    canvas.drawText("\$42.00", width * 0.56f, height * 0.74f, ink)

    canvas.restore()

    FileOutputStream(file).use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    }
    bitmap.recycle()
}

private fun writeDemoPdf(file: File, title: String) {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas
    canvas.drawColor(AndroidColor.WHITE)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(28, 28, 28)
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        textSize = 30f
    }
    canvas.drawText(title, 52f, 88f, paint)

    paint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    paint.textSize = 13f
    paint.color = AndroidColor.rgb(110, 104, 96)
    canvas.drawText("Scanorea document preview", 52f, 114f, paint)

    paint.color = AndroidColor.rgb(56, 52, 48)
    paint.strokeWidth = 2f
    canvas.drawLine(52f, 140f, 543f, 140f, paint)

    paint.strokeWidth = 1f
    paint.color = AndroidColor.rgb(122, 116, 108)
    repeat(18) { index ->
        val y = 178f + index * 30f
        val end = 543f - (index % 3) * 44f
        canvas.drawLine(52f, y, end, y, paint)
    }

    paint.strokeWidth = 2f
    paint.color = AndroidColor.rgb(56, 52, 48)
    canvas.drawRoundRect(RectF(370f, 720f, 543f, 790f), 8f, 8f, paint)
    paint.style = Paint.Style.FILL
    paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    paint.textSize = 16f
    canvas.drawText("TOTAL", 388f, 750f, paint)
    paint.textSize = 22f
    canvas.drawText("\$42.00", 456f, 777f, paint)

    document.finishPage(page)
    FileOutputStream(file).use { output ->
        document.writeTo(output)
    }
    document.close()
}
