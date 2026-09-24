package com.leshoraa.scanorea.features.pdfviewer.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.FileProvider
import com.leshoraa.scanorea.core.util.FileSizeFormatter
import com.leshoraa.scanorea.features.pdfviewer.data.PdfRendererDataSource
import com.leshoraa.scanorea.features.pdfviewer.ui.components.PdfFastScroller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

/**
 * Display modes for PDF viewing:
 * PAGED allows horizontal swiping between full-page views (similar to Google Drive/Adobe Acrobat).
 * CONTINUOUS allows continuous vertical scrolling with fast-scroller thumb.
 */
enum class PdfViewMode {
    PAGED,
    CONTINUOUS
}

/**
 * Built-in native PDF Viewer screen rendering PDF pages adhering to true paper aspect ratios,
 * supporting intuitive horizontal swipe pagination, pinch-to-zoom/pan, and continuous vertical scrolling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    file: File,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pdfRendererSource by remember { mutableStateOf<PdfRendererDataSource?>(null) }
    var totalPages by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Page bitmaps cache
    val pageBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    val listState = rememberLazyListState()

    // Save/Export to custom location launcher
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destinationUri ->
        if (destinationUri != null) {
            coroutineScope.launch {
                copyPdfToDestination(context, file, destinationUri)
            }
        }
    }

    DisposableEffect(file) {
        try {
            val source = PdfRendererDataSource(file)
            pdfRendererSource = source
            totalPages = source.pageCount
        } catch (e: Exception) {
            errorMessage = "Failed to open PDF: ${e.localizedMessage}"
        }

        onDispose {
            pdfRendererSource?.close()
            pageBitmaps.values.forEach { it.recycle() }
            pageBitmaps.clear()
        }
    }

    // View mode: PAGED (horizontal swipe between pages) or CONTINUOUS (vertical scroll)
    var viewMode by remember { mutableStateOf(PdfViewMode.PAGED) }
    val pagerState = rememberPagerState(initialPage = 0) { totalPages }

    // Sync active page position when switching between PAGED and CONTINUOUS modes
    LaunchedEffect(viewMode) {
        if (viewMode == PdfViewMode.CONTINUOUS && totalPages > 0) {
            listState.scrollToItem(pagerState.currentPage)
        } else if (viewMode == PdfViewMode.PAGED && totalPages > 0) {
            pagerState.scrollToPage(listState.firstVisibleItemIndex.coerceIn(0, totalPages - 1))
        }
    }

    // Pinch-to-zoom state for continuous mode
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset = if (scale > 1f) offset + offsetChange else Offset.Zero
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$totalPages Pages • ${FileSizeFormatter.format(file.length())}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (totalPages > 1) {
                        IconButton(
                            onClick = {
                                viewMode = if (viewMode == PdfViewMode.PAGED) PdfViewMode.CONTINUOUS else PdfViewMode.PAGED
                            }
                        ) {
                            Icon(
                                imageVector = if (viewMode == PdfViewMode.PAGED) {
                                    Icons.Outlined.ViewAgenda
                                } else {
                                    Icons.Outlined.ViewCarousel
                                },
                                contentDescription = if (viewMode == PdfViewMode.PAGED) {
                                    "Switch to Continuous Scroll"
                                } else {
                                    "Switch to Page Swipe"
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            saveFileLauncher.launch(file.name)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = "Save to custom folder"
                        )
                    }

                    IconButton(
                        onClick = {
                            sharePdfFile(context, file)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share PDF"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else if (totalPages == 0) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (viewMode == PdfViewMode.PAGED) {
                // Page-by-page horizontal swipe view with pinch-to-zoom and double-tap zoom
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { pageIndex ->
                    val pageNumber = pageIndex + 1
                    val bitmap = pageBitmaps[pageIndex]
                    val pageRatio = pdfRendererSource?.getPageAspectRatio(pageIndex) ?: (1f / 1.4142f)

                    LaunchedEffect(pageIndex) {
                        if (bitmap == null && pdfRendererSource != null) {
                            try {
                                val rendered = pdfRendererSource!!.renderPage(pageIndex, targetWidth = 1080)
                                pageBitmaps[pageIndex] = rendered
                            } catch (_: Exception) {}
                        }
                    }

                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        var pageZoom by remember { mutableFloatStateOf(1f) }
                        var pagePan by remember { mutableStateOf(Offset.Zero) }

                        val availableW = (maxWidth - 32.dp).coerceAtLeast(100.dp)
                        val availableH = (maxHeight - 32.dp).coerceAtLeast(100.dp)

                        val (fittedW, fittedH) = if (pageRatio > 0f) {
                            val containerAspect = availableW / availableH
                            if (pageRatio > containerAspect) {
                                availableW to (availableW / pageRatio)
                            } else {
                                (availableH * pageRatio) to availableH
                            }
                        } else {
                            availableW to availableH
                        }

                        val density = LocalDensity.current
                        val fittedWPx = with(density) { fittedW.toPx() }
                        val fittedHPx = with(density) { fittedH.toPx() }

                        Box(
                            modifier = Modifier
                                .size(fittedW, fittedH)
                                .graphicsLayer {
                                    scaleX = pageZoom
                                    scaleY = pageZoom
                                    translationX = pagePan.x
                                    translationY = pagePan.y
                                }
                                .pointerInput(pageIndex) {
                                    detectTapGestures(
                                        onDoubleTap = { tapOffset ->
                                            if (pageZoom > 1.05f) {
                                                pageZoom = 1f
                                                pagePan = Offset.Zero
                                            } else {
                                                pageZoom = 2.5f
                                                val centerX = size.width / 2f
                                                val centerY = size.height / 2f
                                                val maxPanX = ((fittedWPx * 1.5f) / 2f).coerceAtLeast(0f)
                                                val maxPanY = ((fittedHPx * 1.5f) / 2f).coerceAtLeast(0f)
                                                pagePan = Offset(
                                                    x = ((centerX - tapOffset.x) * 1.5f).coerceIn(-maxPanX, maxPanX),
                                                    y = ((centerY - tapOffset.y) * 1.5f).coerceIn(-maxPanY, maxPanY)
                                                )
                                            }
                                        }
                                    )
                                }
                                .pointerInput(pageIndex) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        var isPinchZoom = false
                                        var prevSpan = 0f
                                        var prevCentroid = Offset.Zero

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val pressed = event.changes.filter { it.pressed }
                                            if (pressed.isEmpty()) break

                                            if (pressed.size >= 2) {
                                                isPinchZoom = true
                                                val p0 = pressed[0].position
                                                val p1 = pressed[1].position
                                                val currentSpan = kotlin.math.hypot(p0.x - p1.x, p0.y - p1.y)
                                                val currentCentroid = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)

                                                if (prevSpan > 0f && currentSpan > 0f) {
                                                    val zoomChange = currentSpan / prevSpan
                                                    val panChange = currentCentroid - prevCentroid

                                                    val newScale = (pageZoom * zoomChange).coerceIn(1f, 4f)
                                                    pageZoom = newScale
                                                    if (newScale > 1f) {
                                                        val maxPanX = ((fittedWPx * (newScale - 1f)) / 2f).coerceAtLeast(0f)
                                                        val maxPanY = ((fittedHPx * (newScale - 1f)) / 2f).coerceAtLeast(0f)
                                                        pagePan = Offset(
                                                            x = (pagePan.x + panChange.x).coerceIn(-maxPanX, maxPanX),
                                                            y = (pagePan.y + panChange.y).coerceIn(-maxPanY, maxPanY)
                                                        )
                                                    } else {
                                                        pagePan = Offset.Zero
                                                    }
                                                }
                                                prevSpan = currentSpan
                                                prevCentroid = currentCentroid
                                                event.changes.forEach { it.consume() }
                                            } else if (pressed.size == 1 && !isPinchZoom) {
                                                if (pageZoom > 1.05f) {
                                                    val change = pressed[0]
                                                    val drag = change.positionChange()
                                                    val maxPanX = ((fittedWPx * (pageZoom - 1f)) / 2f).coerceAtLeast(0f)
                                                    val maxPanY = ((fittedHPx * (pageZoom - 1f)) / 2f).coerceAtLeast(0f)
                                                    pagePan = Offset(
                                                        x = (pagePan.x + drag.x).coerceIn(-maxPanX, maxPanX),
                                                        y = (pagePan.y + drag.y).coerceIn(-maxPanY, maxPanY)
                                                    )
                                                    change.consume()
                                                }
                                                // When pageZoom <= 1.05f, single-finger drag is NOT consumed,
                                                // allowing parent HorizontalPager to swipe smoothly between pages.
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Page $pageNumber",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating page indicator badge in PAGED mode
                if (totalPages > 1) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / $totalPages",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .transformable(state = transformableState)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(count = totalPages) { index ->
                        val pageNumber = index + 1
                        val bitmap = pageBitmaps[index]
                        val pageRatio = pdfRendererSource?.getPageAspectRatio(index) ?: (1f / 1.4142f)

                        LaunchedEffect(index) {
                            if (bitmap == null && pdfRendererSource != null) {
                                try {
                                    val rendered = pdfRendererSource!!.renderPage(index, targetWidth = 1080)
                                    pageBitmaps[index] = rendered
                                } catch (_: Exception) {}
                            }
                        }

                        // Paper sheet rendering adhering to actual paper aspect ratio
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(pageRatio),
                            shape = RoundedCornerShape(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Page $pageNumber",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                }

                // Interactive Fast Scroller & Scrollbar Thumb
                PdfFastScroller(
                    listState = listState,
                    totalPages = totalPages,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

private fun sharePdfFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF Document"))
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to share PDF document.", Toast.LENGTH_SHORT).show()
    }
}

private suspend fun copyPdfToDestination(context: Context, sourceFile: File, destinationUri: Uri) {
    withContext(Dispatchers.IO) {
        try {
            FileInputStream(sourceFile).use { input ->
                context.contentResolver.openOutputStream(destinationUri).use { output ->
                    if (output == null) {
                        throw java.io.IOException("Cannot open output stream for chosen location.")
                    }
                    input.copyTo(output)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "PDF saved successfully!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to save PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
