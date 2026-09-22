package com.leshoraa.scanorea.features.home.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.features.home.ui.components.HomeHeroBanner
import com.leshoraa.scanorea.features.home.ui.components.QuickToolsSection
import com.leshoraa.scanorea.features.recentpdfs.domain.model.RecentPdf
import com.leshoraa.scanorea.features.recentpdfs.ui.RecentPdfsSection
import java.io.File

/**
 * Main dashboard screen displaying hero call-to-action, quick capture tools, and recent results summary.
 */
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
            onDeleteClick = onDeleteClick
        )
    }
}
