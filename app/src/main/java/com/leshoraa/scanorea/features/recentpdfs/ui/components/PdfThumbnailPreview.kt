package com.leshoraa.scanorea.features.recentpdfs.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.features.recentpdfs.data.PdfThumbnailLoader
import java.io.File

/**
 * Renders an asynchronous 1-page visual preview thumbnail of a [file].
 * Displays a static placeholder while loading and falls back to a document icon if unreadable.
 *
 * [lastModifiedMillis] must come from [com.leshoraa.scanorea.features.recentpdfs.domain.model.RecentPdf]
 * metadata so composing the item does not perform a disk stat on the main thread.
 *
 * The placeholder is intentionally static: a shimmer per visible item keeps an animation
 * running for every uncached row, which competes with PDF decoding while the list scrolls.
 */
@Composable
fun PdfThumbnailPreview(
    file: File,
    lastModifiedMillis: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cachedThumbnail = remember(file.absolutePath, lastModifiedMillis) {
        PdfThumbnailLoader.getCachedThumbnail(
            file = file,
            lastModifiedMillis = lastModifiedMillis
        )
    }
    val thumbnailState = produceState<Bitmap?>(
        initialValue = cachedThumbnail,
        key1 = file.absolutePath,
        key2 = lastModifiedMillis
    ) {
        if (value == null) {
            value = PdfThumbnailLoader.loadFirstPageThumbnail(
                file = file,
                diskCacheDir = context.cacheDir,
                lastModifiedMillis = lastModifiedMillis
            )
        }
    }

    val thumbnail = thumbnailState.value

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = "PDF Thumbnail Preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
