package com.leshoraa.scanorea.features.editor.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Parameter adjustment sub-modes in the Adjust tab with fine-grained ruler control.
 */
enum class AdjustmentTool(val displayName: String, val icon: ImageVector) {
    CONTRAST("Contrast", Icons.Outlined.Contrast),
    BRIGHTNESS("Brightness", Icons.Outlined.Brightness6)
}
