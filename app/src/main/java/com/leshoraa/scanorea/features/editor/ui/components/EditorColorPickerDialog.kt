package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.leshoraa.scanorea.features.editor.domain.model.AnnotationColors
import com.leshoraa.scanorea.features.editor.domain.model.HsvColor
import kotlin.math.roundToInt

private val COLOR_PREVIEW_SIZE = 60.dp
private val PALETTE_BOX_HEIGHT = 170.dp
private val SLIDER_TRACK_HEIGHT = 24.dp
private val PRESET_SWATCH_SIZE = 28.dp
private val THUMB_OUTER_RADIUS = 11.dp
private val THUMB_INNER_RADIUS = 9.dp
private val CHECKERBOARD_LIGHT_COLOR = Color.White
private val CHECKERBOARD_DARK_COLOR = Color(0xFFE2E2E2)

private val RAINBOW_HUE_SPECTRUM: List<Color> = listOf(
    Color.Red,
    Color.Yellow,
    Color.Green,
    Color.Cyan,
    Color.Blue,
    Color.Magenta,
    Color.Red
)

/**
 * Material 3 Dialog allowing users to select any custom color and opacity.
 * Uses an intuitive 2D Saturation-Value box, Hue spectrum bar, and Alpha slider.
 */
@Composable
fun EditorColorPickerDialog(
    initialColor: Long,
    initialAlpha: Float,
    onColorConfirmed: (color: Long, alpha: Float) -> Unit,
    onDismissRequest: () -> Unit
) {
    val initialHsv = remember(initialColor, initialAlpha) {
        HsvColor.fromColorLong(initialColor, initialAlpha)
    }

    var selectedHue by remember { mutableFloatStateOf(initialHsv.hue) }
    var selectedSaturation by remember { mutableFloatStateOf(initialHsv.saturation) }
    var selectedValue by remember { mutableFloatStateOf(initialHsv.value) }
    var selectedAlpha by remember { mutableFloatStateOf(initialHsv.alpha) }

    val currentHsvColor = remember(selectedHue, selectedSaturation, selectedValue, selectedAlpha) {
        HsvColor(
            hue = selectedHue,
            saturation = selectedSaturation,
            value = selectedValue,
            alpha = selectedAlpha
        )
    }

    val currentColorLong = remember(currentHsvColor) { currentHsvColor.toColorLong() }
    val solidColor = remember(currentHsvColor) { Color(currentHsvColor.toColorLong()) }
    val currentColorWithAlpha = remember(solidColor, selectedAlpha) { solidColor.copy(alpha = selectedAlpha) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Color & Opacity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(COLOR_PREVIEW_SIZE)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        CheckerboardPattern(modifier = Modifier.fillMaxSize(), squareSizeDp = 6.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(currentColorWithAlpha)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentHsvColor.toHexString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Opacity: ${(selectedAlpha * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnnotationColors.PRESETS.forEach { presetColorLong ->
                        val presetColor = Color(presetColorLong)
                        val isSelected = presetColorLong == currentColorLong
                        Box(
                            modifier = Modifier
                                .size(PRESET_SWATCH_SIZE)
                                .clip(CircleShape)
                                .background(presetColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.25f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    val presetHsv = HsvColor.fromColorLong(presetColorLong, selectedAlpha)
                                    selectedHue = presetHsv.hue
                                    selectedSaturation = presetHsv.saturation
                                    selectedValue = presetHsv.value
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                SaturationValueBox(
                    hue = selectedHue,
                    saturation = selectedSaturation,
                    value = selectedValue,
                    onColorChanged = { newSaturation, newValue ->
                        selectedSaturation = newSaturation
                        selectedValue = newValue
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PALETTE_BOX_HEIGHT)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hue",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${selectedHue.roundToInt()}°",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                HueBar(
                    hue = selectedHue,
                    onHueChanged = { updatedHue -> selectedHue = updatedHue },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Opacity",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(selectedAlpha * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                AlphaBar(
                    alpha = selectedAlpha,
                    baseColor = solidColor,
                    onAlphaChanged = { updatedAlpha -> selectedAlpha = updatedAlpha },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            onColorConfirmed(currentColorLong, selectedAlpha)
                            onDismissRequest()
                        },
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

/**
 * 2D Saturation-Value palette box rendering a dual-gradient canvas.
 */
@Composable
private fun SaturationValueBox(
    hue: Float,
    saturation: Float,
    value: Float,
    onColorChanged: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val pureHueColor = remember(hue) {
        val rgb = HsvColor.hsvToRgb(hue, 1f, 1f)
        Color(rgb.red, rgb.green, rgb.blue)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val initialDown = awaitFirstDown(requireUnconsumed = false)
                    val initialSaturation = (initialDown.position.x / size.width.toFloat()).coerceIn(
                        HsvColor.MIN_FRACTION,
                        HsvColor.MAX_FRACTION
                    )
                    val initialValue = (1f - (initialDown.position.y / size.height.toFloat())).coerceIn(
                        HsvColor.MIN_FRACTION,
                        HsvColor.MAX_FRACTION
                    )
                    onColorChanged(initialSaturation, initialValue)

                    while (true) {
                        val pointerEvent = awaitPointerEvent()
                        val currentChange = pointerEvent.changes.firstOrNull { it.id == initialDown.id } ?: break
                        if (!currentChange.pressed) break
                        currentChange.consume()

                        val updatedSaturation = (currentChange.position.x / size.width.toFloat()).coerceIn(
                            HsvColor.MIN_FRACTION,
                            HsvColor.MAX_FRACTION
                        )
                        val updatedValue = (1f - (currentChange.position.y / size.height.toFloat())).coerceIn(
                            HsvColor.MIN_FRACTION,
                            HsvColor.MAX_FRACTION
                        )
                        onColorChanged(updatedSaturation, updatedValue)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, pureHueColor)
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black)
                )
            )

            val selectorPositionX = saturation * size.width
            val selectorPositionY = (1f - value) * size.height
            val selectorCenter = Offset(selectorPositionX, selectorPositionY)

            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = THUMB_OUTER_RADIUS.toPx(),
                center = selectorCenter,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = THUMB_INNER_RADIUS.toPx(),
                center = selectorCenter,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/**
 * Rainbow Hue spectrum slider bar supporting touch and drag gesture selection.
 */
@Composable
private fun HueBar(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(SLIDER_TRACK_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .horizontalFractionTracker { horizontalFraction ->
                val calculatedHue = (horizontalFraction * HsvColor.MAX_HUE).coerceIn(
                    HsvColor.MIN_HUE,
                    HsvColor.MAX_HUE
                )
                onHueChanged(calculatedHue)
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.horizontalGradient(colors = RAINBOW_HUE_SPECTRUM)
            )

            val thumbPositionX = (hue / HsvColor.MAX_HUE).coerceIn(HsvColor.MIN_FRACTION, HsvColor.MAX_FRACTION) * size.width
            drawContrastThumbIndicator(center = Offset(thumbPositionX, size.height / 2f))
        }
    }
}

/**
 * Alpha opacity slider bar with checkerboard background and live color opacity gradient.
 */
@Composable
private fun AlphaBar(
    alpha: Float,
    baseColor: Color,
    onAlphaChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(SLIDER_TRACK_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .horizontalFractionTracker { horizontalFraction ->
                val calculatedAlpha = horizontalFraction.coerceIn(
                    HsvColor.MIN_ALPHA,
                    HsvColor.MAX_ALPHA
                )
                onAlphaChanged(calculatedAlpha)
            }
    ) {
        CheckerboardPattern(modifier = Modifier.fillMaxSize(), squareSizeDp = 6.dp)

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        baseColor.copy(alpha = HsvColor.MIN_ALPHA),
                        baseColor.copy(alpha = HsvColor.MAX_ALPHA)
                    )
                )
            )

            val thumbPositionX = alpha.coerceIn(HsvColor.MIN_ALPHA, HsvColor.MAX_ALPHA) * size.width
            drawContrastThumbIndicator(center = Offset(thumbPositionX, size.height / 2f))
        }
    }
}

/**
 * Renders a subtle checkerboard pattern to visualize transparency.
 */
@Composable
private fun CheckerboardPattern(
    modifier: Modifier = Modifier,
    squareSizeDp: Dp = 8.dp
) {
    Canvas(modifier = modifier) {
        val squareSizePx = squareSizeDp.toPx()
        val columnCount = (size.width / squareSizePx).toInt() + 1
        val rowCount = (size.height / squareSizePx).toInt() + 1
        for (rowIndex in 0 until rowCount) {
            for (columnIndex in 0 until columnCount) {
                val squareColor = if ((rowIndex + columnIndex) % 2 == 0) {
                    CHECKERBOARD_LIGHT_COLOR
                } else {
                    CHECKERBOARD_DARK_COLOR
                }
                drawRect(
                    color = squareColor,
                    topLeft = Offset(columnIndex * squareSizePx, rowIndex * squareSizePx),
                    size = Size(squareSizePx, squareSizePx)
                )
            }
        }
    }
}

/**
 * Shared modifier extension handling pointer gestures for 1D horizontal slider bars.
 */
private fun Modifier.horizontalFractionTracker(
    onFractionChanged: (Float) -> Unit
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val initialDown = awaitFirstDown(requireUnconsumed = false)
        val initialFraction = (initialDown.position.x / size.width.toFloat()).coerceIn(0f, 1f)
        onFractionChanged(initialFraction)

        while (true) {
            val pointerEvent = awaitPointerEvent()
            val currentChange = pointerEvent.changes.firstOrNull { it.id == initialDown.id } ?: break
            if (!currentChange.pressed) break
            currentChange.consume()

            val updatedFraction = (currentChange.position.x / size.width.toFloat()).coerceIn(0f, 1f)
            onFractionChanged(updatedFraction)
        }
    }
}

/**
 * Draws a high-contrast circular thumb indicator visible against both light and dark backgrounds.
 */
private fun DrawScope.drawContrastThumbIndicator(center: Offset) {
    drawCircle(
        color = Color.Black.copy(alpha = 0.5f),
        radius = THUMB_OUTER_RADIUS.toPx(),
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = THUMB_INNER_RADIUS.toPx(),
        center = center
    )
}
