package com.leshoraa.scanorea.features.presets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.features.imagestopdf.domain.model.CompressionProfile
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageOrientation
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageSize
import com.leshoraa.scanorea.features.presets.domain.TemplateDateEvaluator
import com.leshoraa.scanorea.features.presets.domain.model.ConversionPreset

/**
 * Dedicated Preset Management Screen.
 * Allows users to manage conversion presets and custom tags with a clean full-screen flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetManagementScreen(
    presets: List<ConversionPreset>,
    onBack: () -> Unit,
    onSavePreset: (ConversionPreset) -> Unit,
    onDeletePreset: (presetId: String) -> Unit,
    customTags: Map<String, String> = emptyMap(),
    onSaveCustomTag: (key: String, value: String) -> Unit = { _, _ -> },
    onDeleteCustomTag: (key: String) -> Unit = {},
    studentNim: String = "",
    studentName: String = "",
    studentSubject: String = "",
    onUpdateStudentProfile: (nim: String, name: String, subject: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var isCreatingNew by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<ConversionPreset?>(null) }
    var presetPendingDelete by remember { mutableStateOf<ConversionPreset?>(null) }

    // Merge custom tags with any legacy student profile fields
    val activeCustomTags = remember(customTags, studentNim, studentName, studentSubject) {
        val merged = customTags.toMutableMap()
        if (studentNim.isNotBlank() && !merged.containsKey("NIM")) merged["NIM"] = studentNim
        if (studentName.isNotBlank() && !merged.containsKey("NAMA")) merged["NAMA"] = studentName
        if (studentSubject.isNotBlank() && !merged.containsKey("MATKUL")) merged["MATKUL"] = studentSubject
        merged
    }

    // Full screen navigation: if editing or creating, show dedicated PresetEditScreen
    if (isCreatingNew || editingPreset != null) {
        BackHandler {
            isCreatingNew = false
            editingPreset = null
        }
        PresetEditScreen(
            preset = editingPreset,
            customTags = activeCustomTags,
            onBack = {
                isCreatingNew = false
                editingPreset = null
            },
            onSave = { preset ->
                onSavePreset(preset)
                isCreatingNew = false
                editingPreset = null
            },
            onSaveCustomTag = onSaveCustomTag,
            onDeleteCustomTag = onDeleteCustomTag,
            modifier = modifier
        )
        return
    }

    // Main Preset List Screen
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Preset Templates",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Auto name & page settings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                Button(
                    onClick = { isCreatingNew = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Preset", fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved Presets (${presets.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap to edit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(presets, key = { it.id }) { preset ->
                PresetItemCard(
                    preset = preset,
                    customTags = activeCustomTags,
                    onEditClick = { editingPreset = preset },
                    onDeleteClick = { presetPendingDelete = preset }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Delete Confirmation Dialog
    presetPendingDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetPendingDelete = null },
            title = {
                Text(
                    text = "Delete Preset?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text("Are you sure you want to remove preset '${preset.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePreset(preset.id)
                        presetPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { presetPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Saved Preset Item Card.
 * Card background matches Home screen (surfaceContainerLowest).
 * Subtitle directly displays the evaluated result without raw tokens or "Result today:".
 */
@Composable
private fun PresetItemCard(
    preset: ConversionPreset,
    customTags: Map<String, String>,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val evaluatedPreview = remember(preset.fileNameTemplate, customTags) {
        TemplateDateEvaluator.evaluate(
            template = preset.fileNameTemplate,
            overrideTags = customTags
        ) + ".pdf"
    }

    Card(
        onClick = onEditClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = evaluatedPreview,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetBadge(text = preset.pageSize.label)
                PresetBadge(text = preset.orientation.label)
                PresetBadge(text = preset.compressionProfile.label)
            }
        }
    }
}

@Composable
private fun PresetBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Dedicated full-screen page for creating or editing presets.
 * Clean, spacious, and avoids cramped bottom sheets or popups.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetEditScreen(
    preset: ConversionPreset?,
    customTags: Map<String, String>,
    onBack: () -> Unit,
    onSave: (ConversionPreset) -> Unit,
    onSaveCustomTag: (key: String, value: String) -> Unit,
    onDeleteCustomTag: (key: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(preset?.name ?: "") }
    var template by remember { mutableStateOf(preset?.fileNameTemplate ?: "Doc_{DATE}") }
    var pageSize by remember { mutableStateOf(preset?.pageSize ?: PdfPageSize.A4) }
    var orientation by remember { mutableStateOf(preset?.orientation ?: PdfPageOrientation.PORTRAIT) }
    var compression by remember { mutableStateOf(preset?.compressionProfile ?: CompressionProfile.AUTO_BALANCED) }

    var isAddTagDialogOpen by remember { mutableStateOf(false) }
    var editingTagKey by remember { mutableStateOf<String?>(null) }
    var tagPendingDelete by remember { mutableStateOf<String?>(null) }

    val evaluatedPreview = remember(template, customTags) {
        TemplateDateEvaluator.evaluate(
            template = template.ifBlank { "Untitled" },
            overrideTags = customTags
        ) + ".pdf"
    }

    val isSaveEnabled = name.isNotBlank() && template.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (preset == null) "New Preset" else "Edit Preset",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Set up file names and page defaults",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            val resultPreset = ConversionPreset(
                                id = preset?.id ?: "preset_${System.currentTimeMillis()}",
                                name = name.trim(),
                                fileNameTemplate = template.trim(),
                                pageSize = pageSize,
                                orientation = orientation,
                                compressionProfile = compression
                            )
                            onSave(resultPreset)
                        },
                        enabled = isSaveEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Preset",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Live Preview Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = evaluatedPreview,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Preset Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Preset Name") },
                placeholder = { Text("e.g. Homework, Project Report") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Filename Template
            OutlinedTextField(
                value = template,
                onValueChange = { template = it },
                label = { Text("File Name Rule") },
                placeholder = { Text("e.g. Doc_{DATE}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Custom Tags Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Custom Tags",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = {
                            editingTagKey = null
                            isAddTagDialogOpen = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Tag",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (customTags.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No custom tags yet. Tap '+ Add Tag' to create tags like {PROJECT}, {CLIENT}, or {NAME}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(customTags.keys.toList(), key = { it }) { key ->
                            val value = customTags[key].orEmpty()
                            CustomTagChip(
                                tagKey = key,
                                tagValue = value,
                                onClick = { template += "{$key}" },
                                onLongClick = {
                                    editingTagKey = key
                                    isAddTagDialogOpen = true
                                },
                                onDelete = { tagPendingDelete = key }
                            )
                        }
                    }
                    Text(
                        text = "Tap a tag to insert. Long-press to edit value.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Date Tags Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Date Tags",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    val dateTokens = listOf("{DATE}", "{DD-MM-YYYY}", "{YYYY}", "{MM}", "{DD}")
                    items(dateTokens) { token ->
                        Surface(
                            onClick = { template += token },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = "+ $token",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "Dates update automatically whenever you export a document.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Page Size Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Page Size",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PdfPageSize.entries.forEach { size ->
                        FilterChip(
                            selected = pageSize == size,
                            onClick = { pageSize = size },
                            label = { Text(size.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Orientation Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Orientation",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PdfPageOrientation.entries.forEach { orient ->
                        FilterChip(
                            selected = orientation == orient,
                            onClick = { orientation = orient },
                            label = { Text(orient.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Compression Profile Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Quality",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompressionProfile.entries.forEach { comp ->
                        FilterChip(
                            selected = compression == comp,
                            onClick = { compression = comp },
                            label = { Text(comp.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Add or Edit Custom Tag Dialog
    if (isAddTagDialogOpen) {
        AddEditTagDialog(
            initialKey = editingTagKey ?: "",
            initialValue = editingTagKey?.let { customTags[it] } ?: "",
            isEditing = editingTagKey != null,
            onSave = { key, value ->
                onSaveCustomTag(key, value)
                isAddTagDialogOpen = false
                editingTagKey = null
            },
            onDismiss = {
                isAddTagDialogOpen = false
                editingTagKey = null
            }
        )
    }

    // Delete Tag Confirmation Dialog
    tagPendingDelete?.let { tagKey ->
        AlertDialog(
            onDismissRequest = { tagPendingDelete = null },
            title = {
                Text(
                    text = "Delete Tag?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text("Are you sure you want to remove tag '{$tagKey}'? Presets using this tag will use its key as fallback.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCustomTag(tagKey)
                        tagPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Custom tag chip with click to insert, long-click to edit, and delete action.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomTagChip(
    tagKey: String,
    tagValue: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = tagKey,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (tagValue.isNotBlank()) {
                    Text(
                        text = tagValue,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Delete Tag",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Dialog to add or edit a custom tag and its value.
 */
@Composable
private fun AddEditTagDialog(
    initialKey: String,
    initialValue: String,
    isEditing: Boolean,
    onSave: (key: String, value: String) -> Unit,
    onDismiss: () -> Unit
) {
    var key by remember { mutableStateOf(initialKey) }
    var value by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Tag '{$initialKey}'" else "New Custom Tag",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tags can be inserted into file name rules and will be replaced by their values.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isEditing) {
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' }.uppercase() },
                        label = { Text("Tag Name") },
                        placeholder = { Text("e.g. PROJECT, CLIENT, NIM") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Tag Value") },
                    placeholder = { Text("e.g. Scanorea, Math_101") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalKey = if (isEditing) initialKey else key.trim()
                    if (finalKey.isNotBlank()) {
                        onSave(finalKey, value.trim())
                    }
                },
                enabled = (if (isEditing) true else key.isNotBlank()),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
