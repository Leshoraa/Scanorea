package com.leshoraa.scanorea.features.editor.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level categories available in the document editor studio.
 */
enum class EditorCategory(val label: String, val icon: ImageVector) {
    FILTERS("Filters", Icons.Filled.ColorLens),
    CROP("Crop", Icons.Filled.Crop),
    ADJUST("Adjust", Icons.Filled.Tune),
    MARKUP("Markup", Icons.Filled.Draw)
}
