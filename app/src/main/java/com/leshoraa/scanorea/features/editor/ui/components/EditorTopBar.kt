package com.leshoraa.scanorea.features.editor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Compare
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import com.leshoraa.scanorea.core.format.FileSizeFormatter

/**
 * Clean, dedicated TopAppBar for the Editor Workspace.
 * Houses title, dynamic metadata subtitle, grid overview, add-page launcher,
 * and contextual document manipulation actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    fileName: String,
    pageCount: Int,
    estimatedSizeBytes: Long,
    hasCurrentPage: Boolean,
    onBackClick: () -> Unit,
    onRenameClick: () -> Unit,
    onGridClick: () -> Unit,
    onAddPagesClick: () -> Unit,
    onRotateCurrentPage: () -> Unit,
    onRotateAllPages: () -> Unit,
    onOptionsClick: () -> Unit,
    onDeleteCurrentPage: () -> Unit,
    isComparing: Boolean = false,
    onCompareChange: (Boolean) -> Unit = {},
    canUndo: Boolean = false,
    onUndoClick: () -> Unit = {},
    canRedo: Boolean = false,
    onRedoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isMenuVisible by remember { mutableStateOf(false) }
    var isDeletePageDialogVisible by remember { mutableStateOf(false) }

    if (isDeletePageDialogVisible) {
        AlertDialog(
            onDismissRequest = { isDeletePageDialogVisible = false },
            title = {
                Text(
                    text = "Delete Page?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove this page from the document? All edits and annotations on this page will be discarded.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeletePageDialogVisible = false
                        onDeleteCurrentPage()
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeletePageDialogVisible = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    TopAppBar(
        modifier = modifier,
        title = {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRenameClick)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = fileName.ifBlank { "Scanorea Document" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Rename",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "$pageCount ${if (pageCount == 1) "page" else "pages"}  •  ~${FileSizeFormatter.format(estimatedSizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            // Grid Overview Button
            IconButton(onClick = onGridClick) {
                Icon(
                    imageVector = Icons.Filled.GridView,
                    contentDescription = "Grid Overview",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Sequential Adjustment Undo Button
            IconButton(
                onClick = onUndoClick,
                enabled = canUndo
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo page adjustment",
                    tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            // Sequential Adjustment Redo Button
            IconButton(
                onClick = onRedoClick,
                enabled = canRedo
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo page adjustment",
                    tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            // Hold-to-Compare Button (shows original unedited photo while held).
            // No scale/color animation so the original appears immediately on press.
            if (hasCurrentPage) {
                val haptic = LocalHapticFeedback.current
                val compareContainerColor = if (isComparing) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                }
                val compareIconTint = if (isComparing) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(compareContainerColor)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onCompareChange(true)
                                    try {
                                        tryAwaitRelease()
                                    } finally {
                                        onCompareChange(false)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Compare,
                        contentDescription = "Hold to compare original",
                        tint = compareIconTint
                    )
                }
            }

            // 3-dots Overflow Menu
            Box {
                IconButton(onClick = { isMenuVisible = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                DropdownMenu(
                    expanded = isMenuVisible,
                    onDismissRequest = { isMenuVisible = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Add Pages") },
                        leadingIcon = {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuVisible = false
                            onAddPagesClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename Document") },
                        leadingIcon = {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuVisible = false
                            onRenameClick()
                        }
                    )
                    if (hasCurrentPage) {
                        DropdownMenuItem(
                            text = { Text("Rotate Page (90°)") },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                isMenuVisible = false
                                onRotateCurrentPage()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Rotate All Pages (90°)") },
                        leadingIcon = {
                            Icon(Icons.Filled.ScreenRotation, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuVisible = false
                            onRotateAllPages()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Document Options") },
                        leadingIcon = {
                            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuVisible = false
                            onOptionsClick()
                        }
                    )
                    if (hasCurrentPage) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text("Remove This Page", color = MaterialTheme.colorScheme.error)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                isMenuVisible = false
                                isDeletePageDialogVisible = true
                            }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
