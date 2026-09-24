package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage

/**
 * Floating animated card overlay rendered when a page is being reordered.
 */
@Composable
fun EditorReorderLiftedCard(
    movingPage: ImagePage,
    movingTargetIndex: Int,
    liftProgress: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorMatrix = movingPage.filter.createColorMatrix(movingPage.contrast, movingPage.brightness)
    val colorFilter = colorMatrix?.let { ColorFilter.colorMatrix(ColorMatrix(it.array)) }

    Card(
        modifier = modifier
            .fillMaxSize()
            .zIndex(20f)
            .graphicsLayer {
                translationY = -46.dp.toPx() * liftProgress
                scaleX = 1f + 0.05f * liftProgress
                scaleY = 1f + 0.05f * liftProgress
            }
            .padding(4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(movingPage.uri)
                    .transformations(
                        PagePreviewTransformation(
                            rotationDegrees = movingPage.rotationDegrees,
                            cropBounds = movingPage.cropBounds
                        )
                    )
                    .crossfade(false)
                    .build(),
                contentDescription = movingPage.displayName,
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
                shadowElevation = 0.dp,
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
