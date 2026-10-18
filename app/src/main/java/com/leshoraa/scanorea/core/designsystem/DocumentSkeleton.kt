package com.leshoraa.scanorea.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * Applies an animated shimmering gradient placeholder to a component while data is being fetched.
 */
@Composable
fun Modifier.skeletonShimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "skeletonShimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslation"
    )

    val baseColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlightColor = MaterialTheme.colorScheme.surfaceContainerHighest

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 250f, translateAnim - 250f),
        end = Offset(translateAnim + 250f, translateAnim + 250f)
    )

    return this.background(brush)
}

/**
 * Skeleton placeholder item reflecting the exact structure of a recent PDF document card.
 */
@Composable
fun RecentPdfSkeletonCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document thumbnail skeleton placeholder
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .skeletonShimmer()
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Title line placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.68f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .skeletonShimmer()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata subtitle line placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.38f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .skeletonShimmer()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Action icon circle placeholder
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .skeletonShimmer()
            )
        }
    }
}

/**
 * Skeleton placeholder representing the 2x2 pinned folder grid while loading categories.
 */
@Composable
fun FolderGridSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (i in 0 until 2) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .skeletonShimmer()
                )
            }
        }
    }
}
