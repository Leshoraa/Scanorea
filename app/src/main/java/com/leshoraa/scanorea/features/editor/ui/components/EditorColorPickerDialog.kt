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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.leshoraa.scanorea.features.editor.domain.model.AnnotationColors
import kotlin.math.roundToInt

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
    val initialHsv = remember(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toInt(), hsv)
        hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember { mutableFloatStateOf(initialAlpha.coerceIn(0.05f, 1.0f)) }

    val currentColorInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
    val red = android.graphics.Color.red(currentColorInt)
    val green = android.graphics.Color.green(currentColorInt)
    val blue = android.graphics.Color.blue(currentColorInt)
    val currentColorLong = (0xFFL shl 24) or (red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong()
    val solidColor = Color(red, green, blue)
    val currentColorWithAlpha = solidColor.copy(alpha = alpha)

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
                // Header
                Text(
                    text = "Color & Opacity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Color Preview Box with Hex code and Alpha percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
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
                        val hexString = String.format("#%02X%02X%02X", red, green, blue)
                        Text(
                            text = hexString,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Opacity: ${(alpha * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Preset Swatches for Quick Picking
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnnotationColors.PRESETS.forEach { preset ->
                        val presetColor = Color(preset)
                        val isSelected = preset == currentColorLong
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(presetColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.25f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    val presetHsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(preset.toInt(), presetHsv)
                                    hue = presetHsv[0]
                                    saturation = presetHsv[1]
                                    value = presetHsv[2]
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2D Saturation-Value Color Palette Box
                SaturationValueBox(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onColorChanged = { s, v ->
                        saturation = s
                        value = v
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Rainbow Hue Bar Label & Slider
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
                        text = "${hue.roundToInt()}°",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                HueBar(
                    hue = hue,
                    onHueChanged = { hue = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Alpha Opacity Bar Label & Slider
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
                        text = "${(alpha * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                AlphaBar(
                    alpha = alpha,
                    baseColor = solidColor,
                    onAlphaChanged = { alpha = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Actions
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
                            onColorConfirmed(currentColorLong, alpha)
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
 * 2D Saturation-Value box that renders a dual-gradient canvas.
 * Horizontal gradient transitions from white to pure hue.
 * Vertical gradient transitions from transparent to solid black.
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
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
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
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val sDown = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val vDown = (1f - (down.position.y / size.height.toFloat())).coerceIn(0f, 1f)
                    onColorChanged(sDown, vDown)

                    while (true) {
                        val event = awaitPointerEvent()
                        val current = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!current.pressed) break
                        current.consume()
                        val s = (current.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val v = (1f - (current.position.y / size.height.toFloat())).coerceIn(0f, 1f)
                        onColorChanged(s, v)
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

            val selectorX = saturation * size.width
            val selectorY = (1f - value) * size.height
            val center = Offset(selectorX, selectorY)

            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = 11.dp.toPx(),
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = 9.dp.toPx(),
                center = center,
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
    val rainbowColors = remember {
        listOf(
            Color.Red,
            Color.Yellow,
            Color.Green,
            Color.Cyan,
            Color.Blue,
            Color.Magenta,
            Color.Red
        )
    }

    Box(
        modifier = modifier
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val hDown = ((down.position.x / size.width.toFloat()) * 360f).coerceIn(0f, 360f)
                    onHueChanged(hDown)

                    while (true) {
                        val event = awaitPointerEvent()
                        val current = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!current.pressed) break
                        current.consume()
                        val h = ((current.position.x / size.width.toFloat()) * 360f).coerceIn(0f, 360f)
                        onHueChanged(h)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.horizontalGradient(colors = rainbowColors)
            )

            val thumbX = (hue / 360f).coerceIn(0f, 1f) * size.width
            val center = Offset(thumbX, size.height / 2f)

            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = 11.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 9.dp.toPx(),
                center = center
            )
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
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val aDown = (down.position.x / size.width.toFloat()).coerceIn(0.05f, 1.0f)
                    onAlphaChanged(aDown)

                    while (true) {
                        val event = awaitPointerEvent()
                        val current = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!current.pressed) break
                        current.consume()
                        val a = (current.position.x / size.width.toFloat()).coerceIn(0.05f, 1.0f)
                        onAlphaChanged(a)
                    }
                }
            }
    ) {
        CheckerboardPattern(modifier = Modifier.fillMaxSize(), squareSizeDp = 6.dp)

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.05f),
                        baseColor.copy(alpha = 1.0f)
                    )
                )
            )

            val thumbX = alpha.coerceIn(0.05f, 1.0f) * size.width
            val center = Offset(thumbX, size.height / 2f)

            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = 11.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 9.dp.toPx(),
                center = center
            )
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
        val squareSize = squareSizeDp.toPx()
        val numCols = (size.width / squareSize).toInt() + 1
        val numRows = (size.height / squareSize).toInt() + 1
        for (row in 0 until numRows) {
            for (col in 0 until numCols) {
                val color = if ((row + col) % 2 == 0) Color.White else Color(0xFFE2E2E2)
                drawRect(
                    color = color,
                    topLeft = Offset(col * squareSize, row * squareSize),
                    size = Size(squareSize, squareSize)
                )
            }
        }
    }
}
