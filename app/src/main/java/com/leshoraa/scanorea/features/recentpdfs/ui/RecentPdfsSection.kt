package com.leshoraa.scanorea.features.recentpdfs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.features.recentpdfs.domain.model.RecentPdf
import com.leshoraa.scanorea.features.recentpdfs.ui.components.RecentPdfItemCard
import java.io.File

/**
 * Modern dashboard section for recently generated PDF documents:
 * - Green document icon badge
 * - Clean title and relative time ("2.4 MB • 2 minutes ago")
 * - 3-dots overflow menu (Open, Share, Delete)
 * - Section header with "See All" button
 */
@Composable
fun RecentPdfsSection(
    recentPdfs: List<RecentPdf>,
    onPdfClick: (File) -> Unit,
    onShareClick: (File) -> Unit,
    onDeleteClick: (File) -> Unit,
    modifier: Modifier = Modifier,
    maxDisplayCount: Int = 4,
    onSeeAllClick: (() -> Unit)? = null
) {
    if (recentPdfs.isEmpty()) return

    val displayList = if (maxDisplayCount > 0) recentPdfs.take(maxDisplayCount) else recentPdfs

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Recent Results",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (onSeeAllClick != null && recentPdfs.size > 1) {
                TextButton(
                    onClick = onSeeAllClick,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "See All",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            displayList.forEach { recent ->
                RecentPdfItemCard(
                    recent = recent,
                    onPdfClick = onPdfClick,
                    onShareClick = onShareClick,
                    onDeleteClick = onDeleteClick
                )
            }
        }
    }
}

