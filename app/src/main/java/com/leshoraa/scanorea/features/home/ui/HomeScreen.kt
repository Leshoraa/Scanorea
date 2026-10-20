package com.leshoraa.scanorea.features.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.core.designsystem.ContainedLoadingIndicator
import com.leshoraa.scanorea.features.home.ui.components.HomeHeroBanner
import com.leshoraa.scanorea.features.home.ui.components.QuickToolsSection
import com.leshoraa.scanorea.features.recentpdfs.domain.model.RecentPdf
import com.leshoraa.scanorea.features.recentpdfs.ui.RecentPdfsSection
import java.io.File

/**
 * Main dashboard screen displaying hero call-to-action, quick capture tools, and recent results summary.
 * Features pull-to-refresh driven by the Material 3 Expressive Loading Indicator without any text labels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    recentPdfs: List<RecentPdf>,
    onScanConvertClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onSeeAllToolsClick: () -> Unit,
    onSeeAllResultsClick: () -> Unit,
    onPdfClick: (File) -> Unit,
    onShareClick: (File) -> Unit,
    onDeleteClick: (File) -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: ((File) -> Unit)? = null,
    onMoveToFolderClick: ((File) -> Unit)? = null,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = modifier.fillMaxSize(),
        indicator = {
            if (pullToRefreshState.distanceFraction > 0f || isRefreshing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            val pullDistance = (pullToRefreshState.distanceFraction.coerceIn(0f, 1f) * 80.dp.toPx()) - 56.dp.toPx()
                            translationY = if (isRefreshing) 16.dp.toPx() else pullDistance
                        },
                    contentAlignment = Alignment.Center
                ) {
                    ContainedLoadingIndicator()
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            HomeHeroBanner(onScanConvertClick = onScanConvertClick)

            Spacer(modifier = Modifier.height(6.dp))

            QuickToolsSection(
                onGalleryClick = onGalleryClick,
                onCameraClick = onCameraClick,
                onSeeAllClick = onSeeAllToolsClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            RecentPdfsSection(
                recentPdfs = recentPdfs,
                maxDisplayCount = 3,
                onSeeAllClick = onSeeAllResultsClick,
                onPdfClick = onPdfClick,
                onShareClick = onShareClick,
                onDeleteClick = onDeleteClick,
                onToggleFavorite = onToggleFavorite,
                onMoveToFolderClick = onMoveToFolderClick
            )
        }
    }
}
