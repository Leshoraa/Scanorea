package com.leshoraa.scanorea.core.designsystem

import android.view.ContextThemeWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.R as MaterialR
import com.google.android.material.loadingindicator.LoadingIndicator

/**
 * Official Material 3 Expressive indeterminate loading indicator (uncontained).
 * Powered directly by the official Material Components LoadingIndicator library implementation.
 */
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    indicatorSize: Dp = 38.dp
) {
    val density = LocalDensity.current
    val indicatorSizePx = with(density) { indicatorSize.roundToPx() }
    val colorArgb = color.toArgb()

    AndroidView(
        factory = { context ->
            LoadingIndicator(
                ContextThemeWrapper(context, MaterialR.style.Widget_Material3_LoadingIndicator)
            ).apply {
                setIndicatorColor(colorArgb)
                setIndicatorSize(indicatorSizePx)
                setContainerColor(android.graphics.Color.TRANSPARENT)
                show()
            }
        },
        update = { view ->
            view.setIndicatorColor(colorArgb)
            view.setIndicatorSize(indicatorSizePx)
            view.setContainerColor(android.graphics.Color.TRANSPARENT)
            if (!view.isShown) {
                view.show()
            }
        },
        modifier = modifier
    )
}

/**
 * Official Material 3 Expressive contained loading indicator enclosed in a circular container.
 * Powered directly by the official Material Components LoadingIndicator with Widget.Material3.LoadingIndicator.Contained.
 */
@Composable
fun ContainedLoadingIndicator(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    indicatorColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    containerSize: Dp = 48.dp,
    indicatorSize: Dp = 38.dp
) {
    val density = LocalDensity.current
    val containerSizePx = with(density) { containerSize.roundToPx() }
    val indicatorSizePx = with(density) { indicatorSize.roundToPx() }
    val containerColorArgb = containerColor.toArgb()
    val indicatorColorArgb = indicatorColor.toArgb()

    AndroidView(
        factory = { context ->
            LoadingIndicator(
                ContextThemeWrapper(context, MaterialR.style.Widget_Material3_LoadingIndicator_Contained)
            ).apply {
                setContainerColor(containerColorArgb)
                setIndicatorColor(indicatorColorArgb)
                setContainerWidth(containerSizePx)
                setContainerHeight(containerSizePx)
                setIndicatorSize(indicatorSizePx)
                show()
            }
        },
        update = { view ->
            view.setContainerColor(containerColorArgb)
            view.setIndicatorColor(indicatorColorArgb)
            view.setContainerWidth(containerSizePx)
            view.setContainerHeight(containerSizePx)
            view.setIndicatorSize(indicatorSizePx)
            if (!view.isShown) {
                view.show()
            }
        },
        modifier = modifier
    )
}
