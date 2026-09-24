package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.leshoraa.scanorea.features.editor.domain.model.AnnotationColors
import kotlin.math.roundToInt

/**
 * Material 3 Dialog allowing users to select any custom color (RGB) and opacity (Alpha).
 */
@Composable
fun EditorColorPickerDialog(
    initialColor: Long,
    initialAlpha: Float,
    onColorConfirmed: (color: Long, alpha: Float) -> Unit,
    onDismissRequest: () -> Unit
) {
    val initialRed = ((initialColor shr 16) and 0xFF).toInt()
    val initialGreen = ((initialColor shr 8) and 0xFF).toInt()
    val initialBlue = (initialColor and 0xFF).toInt()

    var red by remember { mutableIntStateOf(initialRed) }
    var green by remember { mutableIntStateOf(initialGreen) }
    var blue by remember { mutableIntStateOf(initialBlue) }
    var alpha by remember { mutableFloatStateOf(initialAlpha.coerceIn(0.1f, 1.0f)) }

    val currentColorLong = (0xFFL shl 24) or (red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong()
    val currentColor = Color(red, green, blue).copy(alpha = alpha)

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

                // Color Preview Box with Alpha Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF222222))
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(currentColor)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        val hexString = String.format("#%02X%02X%02X", red, green, blue)
                        Text(
                            text = hexString,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Opacity: ${(alpha * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    red = ((preset shr 16) and 0xFF).toInt()
                                    green = ((preset shr 8) and 0xFF).toInt()
                                    blue = (preset and 0xFF).toInt()
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Red Slider
                ColorChannelSlider(
                    label = "R",
                    value = red,
                    onValueChange = { red = it },
                    accentColor = Color(0xFFEF5350)
                )

                // Green Slider
                ColorChannelSlider(
                    label = "G",
                    value = green,
                    onValueChange = { green = it },
                    accentColor = Color(0xFF66BB6A)
                )

                // Blue Slider
                ColorChannelSlider(
                    label = "B",
                    value = blue,
                    onValueChange = { blue = it },
                    accentColor = Color(0xFF42A5F5)
                )

                // Opacity Slider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alpha",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp)
                    )
                    Slider(
                        value = alpha,
                        onValueChange = { alpha = it },
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "${(alpha * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.width(36.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor
            )
        )
        Text(
            text = value.toString().padStart(3, ' '),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(42.dp)
        )
    }
}
