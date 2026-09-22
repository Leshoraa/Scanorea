package com.leshoraa.scanorea.features.editor.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level categories available in the document editor studio.
 */
enum class EditorCategory(val label: String, val icon: ImageVector) {
    SUGGESTIONS("Suggestions", Icons.Outlined.AutoFixHigh),
    CROP("Crop", Icons.Outlined.Crop),
    ADJUST("Adjust", Icons.Outlined.Tune),
    FILTERS("Filters", Icons.Outlined.ColorLens)
}
