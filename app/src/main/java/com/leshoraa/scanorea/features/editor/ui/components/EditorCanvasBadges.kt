package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage
import java.util.Locale

/**
 * Overlay badges for the document canvas: top-left [X] page delete tool with filter name,
 * floating zoom reset pill, and top-right page counter.
 */
@Composable
fun BoxScope.EditorCanvasBadges(
    page: ImagePage,
    pageIndex: Int,
    totalPages: Int,
    isCropMode: Boolean,
    isBadgeVisible: Boolean,
    zoomScale: Float,
    onResetZoom: () -> Unit,
    onDeletePage: () -> Unit = {}
) {
    // Top-left Tool Pill: [X] Delete Page button on left, Filter label on right (hidden during crop mode)
    if (!isCropMode) {
        Surface(
            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(start = 4.dp, end = 10.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDeletePage,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Delete Page",
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = page.filter.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    }

    // Floating Zoom Reset Badge / Pill
    AnimatedVisibility(
        visible = zoomScale > 1.05f,
        enter = fadeIn(tween(200)) + scaleIn(tween(200)),
        exit = fadeOut(tween(200)) + scaleOut(tween(200)),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 12.dp, end = if (!isCropMode) 88.dp else 12.dp)
    ) {
        Surface(
            onClick = onResetZoom,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.90f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", zoomScale)}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Reset Zoom",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }

    // Top-right Page Indicator Badge
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(12.dp)
    ) {
        Text(
            text = "${pageIndex + 1} / $totalPages",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
