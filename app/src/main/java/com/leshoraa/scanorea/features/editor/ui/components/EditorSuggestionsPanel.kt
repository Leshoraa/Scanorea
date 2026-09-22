package com.leshoraa.scanorea.features.editor.ui.components

import android.content.ContentResolver
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FilterBAndW
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.core.filter.ImageFilterType
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage

/**
 * Suggestions panel displaying one-tap optimization presets (Auto Clean, B&W Doc, Grayscale, Original).
 */
@Composable
fun EditorSuggestionsPanel(
    page: ImagePage,
    onFilterSelected: (ImageFilterType) -> Unit,
    onAutoAdjust: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SuggestionCard(
            title = "Auto Clean",
            icon = Icons.Outlined.AutoFixHigh,
            isSelected = false,
            onClick = onAutoAdjust
        )

        SuggestionCard(
            title = "B&W Doc",
            icon = Icons.Outlined.Description,
            isSelected = page.filter == ImageFilterType.BLACK_AND_WHITE,
            onClick = { onFilterSelected(ImageFilterType.BLACK_AND_WHITE) }
        )

        SuggestionCard(
            title = "Grayscale",
            icon = Icons.Outlined.FilterBAndW,
            isSelected = page.filter == ImageFilterType.GRAYSCALE,
            onClick = { onFilterSelected(ImageFilterType.GRAYSCALE) }
        )

        SuggestionCard(
            title = "Original",
            icon = Icons.Outlined.ColorLens,
            isSelected = page.filter == ImageFilterType.ORIGINAL,
            onClick = { onFilterSelected(ImageFilterType.ORIGINAL) }
        )
    }
}

@Composable
private fun SuggestionCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1E1E1E),
        modifier = modifier
            .width(104.dp)
            .height(84.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White
            )
        }
    }
}
