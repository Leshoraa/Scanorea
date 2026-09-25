package com.leshoraa.scanorea.features.recentpdfs.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.leshoraa.scanorea.features.recentpdfs.domain.model.RecentPdf
import com.leshoraa.scanorea.features.recentpdfs.ui.components.AllFoldersBottomSheet
import com.leshoraa.scanorea.features.recentpdfs.ui.components.CreateFolderDialog
import com.leshoraa.scanorea.features.recentpdfs.ui.components.DeleteFolderConfirmationDialog
import com.leshoraa.scanorea.features.recentpdfs.ui.components.FolderGridSection
import com.leshoraa.scanorea.features.recentpdfs.ui.components.OrganizeDocumentBottomSheet
import com.leshoraa.scanorea.features.recentpdfs.ui.components.RecentPdfItemCard
import com.leshoraa.scanorea.features.recentpdfs.ui.components.RenameFolderDialog
import java.io.File

/**
 * Filter scope for organizing and viewing documents.
 */
sealed interface DocumentFilter {
    data object All : DocumentFilter
    data object Favorites : DocumentFilter
    data class Category(val name: String) : DocumentFilter
}

/**
 * Dedicated Results screen featuring instant search, 2x2 pinned folder grid,
 * and document management:
 * - Top header with "Results" title, Search action, and options menu.
 * - 2x2 Google Photos style folder grid (up to 3 pinned folders + 1 Other card).
 * - Live search filter for generated PDF files.
 * - Full scrollable list of recent PDF items with favorite toggle and folder organization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    recentPdfs: List<RecentPdf>,
    categories: List<String>,
    pinnedFolders: List<String> = listOf("Favorites", "Work", "Study"),
    onTogglePinFolder: (String) -> Unit = {},
    onPdfClick: (File) -> Unit,
    onShareClick: (File) -> Unit,
    onDeleteClick: (File) -> Unit,
    onToggleFavorite: (File) -> Unit,
    onAssignFolders: (File, List<String>) -> Unit,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (String, String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    modifier: Modifier = Modifier,
    onAssignCategory: ((File, String?) -> Unit)? = null
) {
    var currentFilter by remember { mutableStateOf<DocumentFilter>(DocumentFilter.All) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var sortDescending by remember { mutableStateOf(true) }
    var isAllFoldersSheetVisible by remember { mutableStateOf(false) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<String?>(null) }
    var documentToMove by remember { mutableStateOf<RecentPdf?>(null) }
    var folderToDelete by remember { mutableStateOf<String?>(null) }

    val filteredPdfs = remember(recentPdfs, searchQuery, sortDescending, currentFilter) {
        var list = when (val filter = currentFilter) {
            is DocumentFilter.All -> recentPdfs
            is DocumentFilter.Favorites -> recentPdfs.filter { it.isFavorite }
            is DocumentFilter.Category -> recentPdfs.filter { pdf ->
                pdf.folders.any { it.equals(filter.name, ignoreCase = true) }
            }
        }
        if (searchQuery.isNotBlank()) {
            list = list.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
        }
        if (sortDescending) {
            list.sortedByDescending { it.lastModifiedMillis }
        } else {
            list.sortedBy { it.name.lowercase() }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Results Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "Results",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = { isSearchActive = !isSearchActive }) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search"
                    )
                }

                Box {
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "More options"
                        )
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (sortDescending) "Sort by Name (A-Z)" else "Sort by Newest First") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                showOptionsMenu = false
                                sortDescending = !sortDescending
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Search Bar (shown when toggled)
        if (isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search results...") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // 2x2 Google Photos Style Pinned Folders Grid & Other Card
        FolderGridSection(
            pinnedFolders = pinnedFolders,
            recentPdfs = recentPdfs,
            totalFoldersCount = categories.size,
            currentFilter = currentFilter,
            onSelectFilter = { currentFilter = it },
            onOpenAllFolders = { isAllFoldersSheetVisible = true }
        )

        // Active Filter Banner (shown when filtered by Favorites or Category, with clear button)
        if (currentFilter !is DocumentFilter.All) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val filterLabel = when (val filter = currentFilter) {
                        is DocumentFilter.Favorites -> "Filter: Favorites"
                        is DocumentFilter.Category -> "Folder: ${filter.name}"
                        is DocumentFilter.All -> ""
                    }
                    Text(
                        text = "$filterLabel (${filteredPdfs.size} files)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = { currentFilter = DocumentFilter.All },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Clear filter",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (filteredPdfs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val (icon, title, subtitle) = when {
                        searchQuery.isNotBlank() -> Triple(
                            Icons.Outlined.Search,
                            "No matching documents",
                            "Try searching with a different term"
                        )
                        currentFilter is DocumentFilter.Favorites -> Triple(
                            Icons.Outlined.StarBorder,
                            "No favorites yet",
                            "Tap the star on any document to add it to your favorites"
                        )
                        currentFilter is DocumentFilter.Category -> Triple(
                            Icons.Outlined.FolderOpen,
                            "Folder is empty",
                            "Move documents into \"${(currentFilter as DocumentFilter.Category).name}\" using the document options menu"
                        )
                        else -> Triple(
                            Icons.Outlined.Description,
                            "No results yet",
                            "PDF documents will appear here"
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredPdfs, key = { it.file.absolutePath }) { recent ->
                    RecentPdfItemCard(
                        recent = recent,
                        onPdfClick = onPdfClick,
                        onShareClick = onShareClick,
                        onDeleteClick = onDeleteClick,
                        onToggleFavorite = onToggleFavorite,
                        onMoveToFolderClick = {
                            documentToMove = recent
                        }
                    )
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            existingCategories = categories,
            onConfirm = { name ->
                onAddCategory(name)
                showCreateFolderDialog = false
                documentToMove?.let { doc ->
                    val updatedFolders = (doc.folders + name).distinct()
                    documentToMove = doc.copy(folders = updatedFolders)
                } ?: run {
                    currentFilter = DocumentFilter.Category(name)
                }
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }

    documentToMove?.let { doc ->
        OrganizeDocumentBottomSheet(
            pdf = doc,
            categories = categories,
            onConfirmFolders = { selectedFolders ->
                onAssignFolders(doc.file, selectedFolders)
                onAssignCategory?.invoke(doc.file, selectedFolders.firstOrNull())
                documentToMove = null
            },
            onDismiss = { documentToMove = null },
            onCreateNewFolder = {
                showCreateFolderDialog = true
            }
        )
    }

    folderToRename?.let { oldName ->
        RenameFolderDialog(
            currentName = oldName,
            existingCategories = categories,
            onConfirm = { newName ->
                onRenameCategory(oldName, newName)
                folderToRename = null
                if (currentFilter is DocumentFilter.Category &&
                    (currentFilter as DocumentFilter.Category).name.equals(oldName, ignoreCase = true)
                ) {
                    currentFilter = DocumentFilter.Category(newName)
                }
            },
            onDismiss = { folderToRename = null }
        )
    }

    folderToDelete?.let { folderName ->
        DeleteFolderConfirmationDialog(
            folderName = folderName,
            onConfirm = {
                onDeleteCategory(folderName)
                folderToDelete = null
                if (currentFilter is DocumentFilter.Category &&
                    (currentFilter as DocumentFilter.Category).name.equals(folderName, ignoreCase = true)
                ) {
                    currentFilter = DocumentFilter.All
                }
            },
            onDismiss = { folderToDelete = null }
        )
    }

    if (isAllFoldersSheetVisible) {
        AllFoldersBottomSheet(
            categories = categories,
            pinnedFolders = pinnedFolders,
            recentPdfs = recentPdfs,
            currentFilter = currentFilter,
            onSelectFilter = { currentFilter = it },
            onTogglePinFolder = onTogglePinFolder,
            onCreateNewFolder = {
                isAllFoldersSheetVisible = false
                showCreateFolderDialog = true
            },
            onRenameFolder = { folderToRename = it },
            onDeleteFolder = { folderToDelete = it },
            onDismiss = { isAllFoldersSheetVisible = false }
        )
    }
}
