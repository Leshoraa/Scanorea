package com.leshoraa.scanorea.features.recentpdfs.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.R
import com.leshoraa.scanorea.features.recentpdfs.domain.model.RecentPdf

private const val MAX_FOLDER_NAME_LENGTH = 30
private val INVALID_FOLDER_CHARS = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')

/**
 * Dialog to create a new folder/category for organizing documents.
 * Includes real-time validation against reserved names, duplicates, and forbidden characters.
 */
@Composable
fun CreateFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    existingCategories: List<String> = emptyList()
) {
    var folderName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val trimmed = folderName.trim()
    val isAlreadyExists = trimmed.isNotEmpty() && existingCategories.any { it.equals(trimmed, ignoreCase = true) }
    val isReservedName = trimmed.equals("All", ignoreCase = true) || trimmed.equals("Favorites", ignoreCase = true)
    val hasInvalidChars = folderName.any { it in INVALID_FOLDER_CHARS }

    val errorMessage = when {
        isAlreadyExists -> stringResource(R.string.folder_error_already_exists)
        isReservedName -> stringResource(R.string.folder_error_reserved)
        hasInvalidChars -> stringResource(R.string.folder_error_invalid_chars)
        else -> null
    }
    val isValid = trimmed.isNotEmpty() && errorMessage == null

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.CreateNewFolder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.folder_new_folder),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.folder_create_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it.take(MAX_FOLDER_NAME_LENGTH) },
                    placeholder = { Text(stringResource(R.string.folder_name_placeholder)) },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { errorText ->
                        { Text(errorText, color = MaterialTheme.colorScheme.error) }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        onConfirm(trimmed)
                    }
                },
                enabled = isValid
            ) {
                Text(stringResource(R.string.folder_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.folder_cancel))
            }
        },
        modifier = modifier
    )
}

/**
 * Dialog to rename an existing folder/category.
 * Includes real-time validation against duplicate names, reserved words, and forbidden characters.
 */
@Composable
fun RenameFolderDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    existingCategories: List<String> = emptyList()
) {
    var folderName by remember { mutableStateOf(currentName) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val trimmed = folderName.trim()
    val isSameAsCurrent = trimmed.equals(currentName, ignoreCase = true)
    val isAlreadyExists = trimmed.isNotEmpty() && !isSameAsCurrent && existingCategories.any { it.equals(trimmed, ignoreCase = true) }
    val isReservedName = trimmed.equals("All", ignoreCase = true) || trimmed.equals("Favorites", ignoreCase = true)
    val hasInvalidChars = folderName.any { it in INVALID_FOLDER_CHARS }

    val errorMessage = when {
        isAlreadyExists -> stringResource(R.string.folder_error_already_exists)
        isReservedName -> stringResource(R.string.folder_error_reserved)
        hasInvalidChars -> stringResource(R.string.folder_error_invalid_chars)
        else -> null
    }
    val isValid = trimmed.isNotEmpty() && !isSameAsCurrent && errorMessage == null

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.folder_rename_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.folder_rename_description, currentName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it.take(MAX_FOLDER_NAME_LENGTH) },
                    placeholder = { Text(stringResource(R.string.folder_name_placeholder)) },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { errorText ->
                        { Text(errorText, color = MaterialTheme.colorScheme.error) }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        onConfirm(trimmed)
                    }
                },
                enabled = isValid
            ) {
                Text(stringResource(R.string.folder_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.folder_cancel))
            }
        },
        modifier = modifier
    )
}

/**
 * Google-style Material 3 ModalBottomSheet to organize a document across multiple folders.
 * Supports multi-folder assignment, quick "+ New Folder" creation, and clean Checkbox toggling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizeDocumentBottomSheet(
    pdf: RecentPdf,
    categories: List<String>,
    onConfirmFolders: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    onCreateNewFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedFolders by remember(pdf, categories) {
        mutableStateOf(pdf.folders.toSet())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header: Document preview & sheet title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.DriveFileMove,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.folder_organize_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = pdf.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action: New Folder row (Native Google app style: flat, clean, uncontained)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCreateNewFolder() }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CreateNewFolder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.folder_new_folder),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.5.dp
            )

            // Folders list with Material 3 Checkboxes (Native Google list layout)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (categories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.folder_empty_message),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    categories.forEach { category ->
                        val isChecked = selectedFolders.contains(category)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFolders = if (isChecked) {
                                        selectedFolders - category
                                    } else {
                                        selectedFolders + category
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedFolders = if (checked) {
                                        selectedFolders + category
                                    } else {
                                        selectedFolders - category
                                    }
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: "Clear All", "Cancel", and "Done"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedFolders.isNotEmpty()) {
                    TextButton(onClick = { selectedFolders = emptySet() }) {
                        Text(stringResource(R.string.folder_clear_all))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.folder_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onConfirmFolders(selectedFolders.toList()) }
                ) {
                    Text(stringResource(R.string.folder_done))
                }
            }
        }
    }
}


/**
 * Confirmation dialog for folder deletion.
 */
@Composable
fun DeleteFolderConfirmationDialog(
    folderName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.folder_delete_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.folder_delete_confirmation, folderName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.folder_delete),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.folder_cancel))
            }
        },
        modifier = modifier
    )
}
