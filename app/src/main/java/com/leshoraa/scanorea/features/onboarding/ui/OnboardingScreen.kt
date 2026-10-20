package com.leshoraa.scanorea.features.onboarding.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leshoraa.scanorea.R
import kotlinx.coroutines.launch

/**
 * First-launch guide presented as a swipeable carousel.
 *
 * Each slide pairs a short explanation with a live preview of the real Scanorea
 * UI so users can recognise the exact screens and controls when they reach them.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onSkip: () -> Unit = onFinish,
    modifier: Modifier = Modifier
) {
    val slides = remember { guideSlides() }
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == slides.lastIndex

    BackHandler {
        if (pagerState.currentPage > 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        } else {
            onSkip()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                GuideTopBar(onSkip = onSkip)

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    beyondViewportPageCount = 1
                ) { pageIndex ->
                    GuidePage(
                        slide = slides[pageIndex],
                        onSwipeNext = {
                            if (pagerState.currentPage < slides.lastIndex) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        onSwipePrevious = {
                            if (pagerState.currentPage > 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        }
                    )
                }

                GuideBottomBar(
                    pageCount = slides.size,
                    currentPage = pagerState.currentPage,
                    isLastPage = isLastPage,
                    onNext = {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GuideTopBar(onSkip: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colorResource(R.color.ic_launcher_background)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.55f)
            )
        }

        Spacer(modifier = Modifier.width(9.dp))

        Column {
            Text(
                text = "Scanorea",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "QUICK GUIDE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = onSkip) {
            Text("Skip")
        }
    }
}

@Composable
private fun GuidePage(
    slide: GuideSlide,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val minPageHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minPageHeight)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                slide.preview(Modifier.fillMaxSize())
                PreviewGestureLayer(
                    onSwipeNext = onSwipeNext,
                    onSwipePrevious = onSwipePrevious,
                    modifier = Modifier.matchParentSize()
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = slide.eyebrow,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = slide.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = slide.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slide.tips.forEach { tip ->
                    GuideTipRow(text = tip)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun GuideTipRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(18.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(11.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.width(9.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun GuideBottomBar(
    pageCount: Int,
    currentPage: Int,
    isLastPage: Boolean,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { index ->
                val isSelected = currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (isSelected) 22.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text = if (isLastPage) "Start scanning" else "Next",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (isLastPage) {
                    Icons.Filled.Check
                } else {
                    Icons.AutoMirrored.Filled.ArrowForward
                },
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private data class GuideSlide(
    val eyebrow: String,
    val title: String,
    val description: String,
    val tips: List<String>,
    val preview: @Composable (Modifier) -> Unit
)

private fun guideSlides(): List<GuideSlide> = listOf(
    GuideSlide(
        eyebrow = "WELCOME",
        title = "Your pocket document scanner",
        description = "Turn phone photos into clean, shareable PDFs in a few taps. Everything stays on your device.",
        tips = listOf(
            "Scan with the camera or import from Gallery",
            "No account and no cloud upload",
            "Your files are processed locally"
        ),
        preview = { modifier -> HomePreview(modifier) }
    ),
    GuideSlide(
        eyebrow = "BUILD",
        title = "Add pages, then get the order right",
        description = "Swipe through your document like a carousel. The last card is always ready for another page.",
        tips = listOf(
            "Tap the Add Page card to append more images",
            "Open Grid to reorder or delete pages",
            "Drag pages in Grid until the order feels right"
        ),
        preview = { modifier -> PagesPreview(modifier) }
    ),
    GuideSlide(
        eyebrow = "ENHANCE",
        title = "Make every page easy to read",
        description = "Start with B&W for text, then fine-tune contrast and brightness until the page feels crisp.",
        tips = listOf(
            "B&W is tuned for documents and faint text",
            "Apply a filter to one page or all pages",
            "Sharpness brings small print back into focus"
        ),
        preview = { modifier -> FiltersPreview(modifier) }
    ),
    GuideSlide(
        eyebrow = "STRAIGHTEN",
        title = "Fix perspective and crop the edges",
        description = "Drag the four corners to match the paper. Auto-detect gives you a quick head start.",
        tips = listOf(
            "Use Perspective for angled or tilted photos",
            "Drag each corner until the page looks flat",
            "Tap Apply to bake in the correction"
        ),
        preview = { modifier -> PerspectivePreview(modifier) }
    ),
    GuideSlide(
        eyebrow = "ORGANIZE",
        title = "Folders that keep results tidy",
        description = "Pin the folders you use most, then move finished PDFs into them right from Results.",
        tips = listOf(
            "Pin up to three folders to the top grid",
            "Open a document's menu to change folders",
            "Favorites and search find files fast"
        ),
        preview = { modifier -> FoldersPreview(modifier) }
    ),
    GuideSlide(
        eyebrow = "EXPORT",
        title = "Save it your way",
        description = "Choose paper size, orientation, compression, and a destination before creating the PDF.",
        tips = listOf(
            "Compression shows an estimated file size",
            "Save presets for recurring documents",
            "Open, print, or share from the viewer"
        ),
        preview = { modifier -> ExportPreview(modifier) }
    )
)
