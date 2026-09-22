package com.leshoraa.scanorea.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.leshoraa.scanorea.app.navigation.MainNavTab
import com.leshoraa.scanorea.core.util.FileSizeFormatter
import com.leshoraa.scanorea.core.util.PdfShareUtil
import com.leshoraa.scanorea.features.editor.ui.EditorWorkspace
import com.leshoraa.scanorea.features.editor.ui.components.EditorPagesGridDialog
import com.leshoraa.scanorea.features.home.ui.HomeScreen
import com.leshoraa.scanorea.features.imagestopdf.ui.ImagesToPdfViewModel
import com.leshoraa.scanorea.features.imagestopdf.ui.components.ConversionOptionsBottomSheet
import com.leshoraa.scanorea.features.imagestopdf.ui.components.ConversionProgressDialog
import com.leshoraa.scanorea.features.imagestopdf.ui.components.ConversionSuccessDialog
import com.leshoraa.scanorea.features.pdfviewer.ui.PdfViewerScreen
import com.leshoraa.scanorea.features.presets.domain.TemplateDateEvaluator
import com.leshoraa.scanorea.features.presets.domain.model.ConversionPreset
import com.leshoraa.scanorea.features.recentpdfs.ui.ResultsScreen
import com.leshoraa.scanorea.features.settings.ui.SettingsScreen
import com.leshoraa.scanorea.features.tools.ui.ToolsScreen
import java.io.File

/**
 * Primary host screen managing navigation routing, workspace transitions, and system integrations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ImagesToPdfViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(MainNavTab.HOME) }
    var isRenameDialogOpen by remember { mutableStateOf(false) }
    var isPagesGridVisible by remember { mutableStateOf(false) }
    var showEditorMenu by remember { mutableStateOf(false) }

    // Full-screen native PDF viewer
    val activePdf = uiState.activePdfViewerFile
    if (activePdf != null) {
        BackHandler { viewModel.closePdfViewer() }
        PdfViewerScreen(
            file = activePdf,
            onBackClick = { viewModel.closePdfViewer() }
        )
        return
    }

    // Back button handling
    if (uiState.hasPages) {
        BackHandler { viewModel.clearAllPages() }
    } else if (currentTab != MainNavTab.HOME) {
        BackHandler { currentTab = MainNavTab.HOME }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris, context.contentResolver)
        }
    }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            viewModel.addImages(listOf(tempCameraUri!!), context.contentResolver)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
            val doc = DocumentFile.fromTreeUri(context, uri)
            val displayName = doc?.name ?: "Custom Folder"
            viewModel.setCustomDestinationFolder(uri, displayName)
            Toast.makeText(context, "Save location set to: $displayName", Toast.LENGTH_SHORT).show()
        }
    }

    fun openCamera() {
        try {
            val cachePdfs = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val tempFile = File.createTempFile("scan_", ".jpg", cachePdfs)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { uiState.pages.size }
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.hasPages) {
                val estimatedBytes = uiState.options.compressionProfile.estimateSizeBytes(uiState.pageCount)
                val currentPage = uiState.pages.getOrNull(pagerState.currentPage)

                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearAllPages() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back to Home",
                                tint = Color.White
                            )
                        }
                    },
                    title = {
                        Column(
                            modifier = Modifier.clickable { isRenameDialogOpen = true }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.options.fileName.ifBlank { "Scanorea Document" },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Rename",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "${uiState.pageCount} ${if (uiState.pageCount == 1) "page" else "pages"}  •  ~${FileSizeFormatter.format(estimatedBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    },
                    actions = {
                        // Grid Overview Button
                        IconButton(onClick = { isPagesGridVisible = true }) {
                            Icon(
                                imageVector = Icons.Outlined.GridView,
                                contentDescription = "Grid Overview",
                                tint = Color.White
                            )
                        }

                        // Add Photos Button
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddPhotoAlternate,
                                contentDescription = "Add photos",
                                tint = Color.White
                            )
                        }

                        // 3-dots Overflow Menu
                        Box {
                            IconButton(onClick = { showEditorMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "More Options",
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = showEditorMenu,
                                onDismissRequest = { showEditorMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename Document") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                                    },
                                    onClick = {
                                        showEditorMenu = false
                                        isRenameDialogOpen = true
                                    }
                                )
                                if (currentPage != null) {
                                    DropdownMenuItem(
                                        text = { Text("Rotate Page (90°)") },
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Outlined.RotateRight, contentDescription = null, modifier = Modifier.size(20.dp))
                                        },
                                        onClick = {
                                            showEditorMenu = false
                                            viewModel.rotatePage(currentPage.id)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Rotate All Pages (90°)") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.ScreenRotation, contentDescription = null, modifier = Modifier.size(20.dp))
                                    },
                                    onClick = {
                                        showEditorMenu = false
                                        viewModel.rotateAllPages()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Document Options") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(20.dp))
                                    },
                                    onClick = {
                                        showEditorMenu = false
                                        viewModel.showOptionsBottomSheet(true)
                                    }
                                )
                                if (currentPage != null) {
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = {
                                            Text("Remove This Page", color = MaterialTheme.colorScheme.error)
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        },
                                        onClick = {
                                            showEditorMenu = false
                                            viewModel.removePage(currentPage.id)
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0C0C0C)
                    )
                )
            } else if (currentTab == MainNavTab.HOME) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Scanorea",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Scan  •  Convert  •  Enhance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Scanorea: All features unlocked & ad-free!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WorkspacePremium,
                                contentDescription = "Pro Status",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = { currentTab = MainNavTab.SETTINGS }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (!uiState.hasPages) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp
                ) {
                    MainNavTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!uiState.hasPages) {
                when (currentTab) {
                    MainNavTab.HOME -> {
                        HomeScreen(
                            recentPdfs = uiState.recentPdfs,
                            onScanConvertClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onGalleryClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onCameraClick = { openCamera() },
                            onSeeAllToolsClick = { currentTab = MainNavTab.TOOLS },
                            onSeeAllResultsClick = { currentTab = MainNavTab.RESULTS },
                            onPdfClick = { viewModel.openPdfInViewer(it) },
                            onShareClick = { PdfShareUtil.sharePdf(context, it) },
                            onDeleteClick = { viewModel.deleteRecentPdf(it) }
                        )
                    }

                    MainNavTab.TOOLS -> {
                        ToolsScreen(
                            onGalleryClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onCameraClick = { openCamera() },
                            onPresetsClick = { viewModel.showOptionsBottomSheet(true) }
                        )
                    }

                    MainNavTab.RESULTS -> {
                        ResultsScreen(
                            recentPdfs = uiState.recentPdfs,
                            onPdfClick = { viewModel.openPdfInViewer(it) },
                            onShareClick = { PdfShareUtil.sharePdf(context, it) },
                            onDeleteClick = { viewModel.deleteRecentPdf(it) }
                        )
                    }

                    MainNavTab.SETTINGS -> {
                        SettingsScreen(
                            destinationFolderDisplayName = uiState.destinationFolderDisplayName,
                            onChangeDestinationFolder = { folderPickerLauncher.launch(null) }
                        )
                    }
                }
            } else {
                val estimatedBytes = uiState.options.compressionProfile.estimateSizeBytes(uiState.pageCount)
                EditorWorkspace(
                    pages = uiState.pages,
                    pagerState = pagerState,
                    estimatedSizeBytes = estimatedBytes,
                    onMovePage = { sourceIndex, targetIndex -> viewModel.movePage(sourceIndex, targetIndex) },
                    onRemovePage = { viewModel.removePage(it) },
                    onFilterSelected = { pageId, filter -> viewModel.updatePageFilter(pageId, filter) },
                    onApplyFilterToAll = { filter -> viewModel.applyFilterToAll(filter) },
                    onAdjustmentChange = { pageId, contrast, brightness ->
                        viewModel.updatePageAdjustment(pageId, contrast, brightness)
                    },
                    onAutoAdjustPage = { pageId, resolver ->
                        viewModel.autoAdjustPage(pageId, resolver)
                    },
                    onResetAdjustments = { pageId ->
                        viewModel.resetPageAdjustment(pageId)
                    },
                    onCropChange = { pageId, bounds ->
                        viewModel.updatePageCrop(pageId, bounds)
                    },
                    onRotatePage = { pageId ->
                        viewModel.rotatePage(pageId)
                    },
                    onResetCrop = { pageId ->
                        viewModel.resetPageCrop(pageId)
                    },
                    onCancel = { viewModel.clearAllPages() },
                    onOpenOptionsSheet = { viewModel.showOptionsBottomSheet(true) }
                )
            }
        }
    }

    // Modal Bottom Sheet for Pages Grid Overview
    if (isPagesGridVisible) {
        EditorPagesGridDialog(
            pages = uiState.pages,
            currentPageIndex = pagerState.currentPage,
            onPageSelected = { selectedIndex ->
                coroutineScope.launch {
                    pagerState.scrollToPage(selectedIndex)
                }
            },
            onMovePage = { src, tgt ->
                viewModel.movePage(src, tgt)
                coroutineScope.launch {
                    pagerState.scrollToPage(tgt)
                }
            },
            onRemovePage = { pageId -> viewModel.removePage(pageId) },
            onRotateAllPages = { viewModel.rotateAllPages() },
            onAddPagesClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismissRequest = { isPagesGridVisible = false }
        )
    }

    // Document Rename Dialog
    if (isRenameDialogOpen) {
        var tempFileName by remember(uiState.options.fileName) {
            mutableStateOf(uiState.options.fileName.ifBlank { "Scanorea Document" })
        }
        var selectedPreset by remember { mutableStateOf<ConversionPreset?>(null) }
        var presetsDropdownExpanded by remember { mutableStateOf(false) }
        var showSavePresetDialog by remember { mutableStateOf(false) }
        var newPresetName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { isRenameDialogOpen = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Rename Document",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter a file name or choose a preset template with dynamic date tokens.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // TextField with Presets Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = tempFileName,
                            onValueChange = {
                                tempFileName = it
                                selectedPreset = null
                            },
                            label = { Text("File Name") },
                            placeholder = { Text("e.g. Scanned_Doc_{DD:MM:YYYY}", maxLines = 1) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { presetsDropdownExpanded = !presetsDropdownExpanded }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Presets",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )

                        DropdownMenu(
                            expanded = presetsDropdownExpanded,
                            onDismissRequest = { presetsDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Text(
                                text = "Saved Presets",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            uiState.presets.forEach { preset ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = preset.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = preset.fileNameTemplate,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (preset.id != "preset_aljabar") {
                                            IconButton(
                                                onClick = { viewModel.deletePreset(preset.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = "Delete preset",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedPreset = preset
                                        tempFileName = TemplateDateEvaluator.evaluate(preset.fileNameTemplate)
                                        presetsDropdownExpanded = false
                                    }
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Save Current as New Preset...",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    presetsDropdownExpanded = false
                                    showSavePresetDialog = true
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempFileName.isNotBlank()) {
                            val finalName = TemplateDateEvaluator.evaluate(tempFileName.trim())
                            selectedPreset?.let { preset ->
                                viewModel.applyPreset(preset)
                            }
                            viewModel.updateFileName(finalName)
                        }
                        isRenameDialogOpen = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { isRenameDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )

        // Nested Save Preset Dialog
        if (showSavePresetDialog) {
            AlertDialog(
                onDismissRequest = { showSavePresetDialog = false },
                title = { Text("Save as Preset") },
                text = {
                    Column {
                        Text(
                            text = "Save current file name as a quick preset for future documents.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newPresetName,
                            onValueChange = { newPresetName = it },
                            label = { Text("Preset Name") },
                            placeholder = { Text("e.g. Aljabar, Homework") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPresetName.isNotBlank()) {
                                val template = tempFileName.ifBlank { "Document_{DD:MM:YYYY}" }
                                viewModel.saveNewPreset(newPresetName.trim(), template)
                                newPresetName = ""
                                showSavePresetDialog = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSavePresetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Modal Bottom Sheet for Conversion Options, Presets, and Save Location
    if (uiState.isOptionsBottomSheetVisible) {
        ConversionOptionsBottomSheet(
            options = uiState.options,
            pageCount = uiState.pageCount,
            presets = uiState.presets,
            destinationFolderDisplayName = uiState.destinationFolderDisplayName,
            sheetState = bottomSheetState,
            onDismissRequest = { viewModel.showOptionsBottomSheet(false) },
            onFileNameChange = { viewModel.updateFileName(it) },
            onPageSizeChange = { viewModel.updatePageSize(it) },
            onOrientationChange = { viewModel.updateOrientation(it) },
            onCompressionProfileChange = { viewModel.updateCompressionProfile(it) },
            onPresetSelected = { viewModel.applyPreset(it) },
            onSaveNewPreset = { name, template -> viewModel.saveNewPreset(name, template) },
            onDeletePreset = { viewModel.deletePreset(it) },
            onChangeFolderClick = { folderPickerLauncher.launch(null) },
            onConvertClick = {
                viewModel.showOptionsBottomSheet(false)
                viewModel.convertImagesToPdf()
            }
        )
    }

    // Progress Dialog during PDF generation
    if (uiState.isConverting) {
        ConversionProgressDialog(progress = uiState.conversionProgress)
    }

    // Success Dialog on conversion completion
    val successResult = uiState.conversionResult
    if (successResult != null) {
        ConversionSuccessDialog(
            result = successResult,
            onOpenPdf = {
                val file = successResult.file
                viewModel.dismissResultDialog()
                viewModel.openPdfInViewer(file)
            },
            onSharePdf = {
                PdfShareUtil.sharePdf(context, successResult.file)
            },
            onDismiss = { viewModel.dismissResultDialog() }
        )
    }
}
