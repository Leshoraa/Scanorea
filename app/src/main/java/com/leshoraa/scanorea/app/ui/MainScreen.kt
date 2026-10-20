package com.leshoraa.scanorea.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leshoraa.scanorea.app.navigation.MainNavTab
import com.leshoraa.scanorea.app.ui.components.MainScreenModals
import com.leshoraa.scanorea.app.ui.components.rememberMainScreenLaunchers
import com.leshoraa.scanorea.core.share.PdfDocumentSharer
import com.leshoraa.scanorea.features.editor.ui.EditorWorkspace
import com.leshoraa.scanorea.features.editor.ui.components.EditorTopBar
import com.leshoraa.scanorea.features.home.ui.HomeScreen
import com.leshoraa.scanorea.features.imagestopdf.ui.ImagesToPdfViewModel
import com.leshoraa.scanorea.features.pdfviewer.ui.PdfViewerScreen
import com.leshoraa.scanorea.features.presets.ui.PresetManagementScreen
import com.leshoraa.scanorea.features.recentpdfs.ui.RecentPdfsViewModel
import com.leshoraa.scanorea.features.recentpdfs.ui.ResultsScreen
import com.leshoraa.scanorea.features.settings.ui.SettingsScreen
import com.leshoraa.scanorea.features.tools.ui.ToolsScreen
import kotlinx.coroutines.launch

/**
 * Primary host screen managing navigation routing, workspace transitions, and system integrations.
 * Refactored into a high-level orchestrator delegating sub-components to specialized composables.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ImagesToPdfViewModel,
    recentPdfsViewModel: RecentPdfsViewModel = viewModel(
        factory = RecentPdfsViewModel.provideFactory(LocalContext.current)
    ),
    pendingEntry: AppEntryRequest? = null,
    onEntryConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val recentPdfsUiState by recentPdfsViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val tabs = remember { MainNavTab.entries }
    val tabPagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size }
    )
    val coroutineScope = rememberCoroutineScope()
    val currentTab = tabs[tabPagerState.currentPage]

    var isRenameDialogOpen by remember { mutableStateOf(false) }
    var isPagesGridVisible by remember { mutableStateOf(false) }
    var isPresetManagerVisible by remember { mutableStateOf(false) }
    var isDiscardDialogVisible by remember { mutableStateOf(false) }
    var captureSourceSheetTitle by remember { mutableStateOf<String?>(null) }
    var isComparingOriginal by remember { mutableStateOf(false) }

    LaunchedEffect(tabPagerState.settledPage) {
        val tab = tabs[tabPagerState.settledPage]
        // Only trigger a load when navigating to data-dependent tabs and the list is empty.
        // RecentPdfsViewModel already loads on init; this only covers the case where
        // the user reaches these tabs before the initial load completes.
        if (recentPdfsUiState.recentPdfs.isEmpty() &&
            (tab == MainNavTab.RESULTS || tab == MainNavTab.HOME)
        ) {
            recentPdfsViewModel.loadRecentPdfs()
        }
    }

    val launchers = rememberMainScreenLaunchers(viewModel, context)

    LaunchedEffect(pendingEntry) {
        val entry = pendingEntry ?: return@LaunchedEffect

        isPresetManagerVisible = false
        isPagesGridVisible = false
        isRenameDialogOpen = false
        isDiscardDialogVisible = false
        captureSourceSheetTitle = null
        viewModel.closePdfViewer()

        when {
            entry.imageUris.isNotEmpty() -> {
                viewModel.addImages(entry.imageUris, context.contentResolver)
            }

            entry.pdfUri != null -> {
                viewModel.openPdfFromExternalUri(
                    uri = entry.pdfUri,
                    contentResolver = context.contentResolver,
                    cacheDir = context.cacheDir
                )
            }

            entry.action == AppEntryAction.SCAN_DOCUMENT -> launchers.launchCamera()
            entry.action == AppEntryAction.IMPORT_PHOTOS -> launchers.launchPhotoPicker()
            entry.action == AppEntryAction.OPEN_PDF -> launchers.launchOpenPdf()
        }

        onEntryConsumed()
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
        pageCount = {
            // Always append one extra carousel page as an "Add Page" slot while
            // the document has pages. It is not part of the document page list.
            val pageCount = uiState.pages.size
            if (pageCount > 0) pageCount + 1 else 0
        }
    )

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

    // Full-screen dedicated preset templates manager
    if (isPresetManagerVisible) {
        BackHandler { isPresetManagerVisible = false }
        PresetManagementScreen(
            presets = uiState.presets,
            onBack = { isPresetManagerVisible = false },
            onSavePreset = { viewModel.savePreset(it) },
            onDeletePreset = { viewModel.deletePreset(it) }
        )
        return
    }

    // Back button handling: Intercept unsaved changes with Discard confirmation dialog
    if (uiState.hasPages) {
        BackHandler { isDiscardDialogVisible = true }
    } else if (tabPagerState.currentPage != 0) {
        BackHandler {
            coroutineScope.launch {
                tabPagerState.scrollToPage(0)
            }
        }
    }

    val isDark = isSystemInDarkTheme()
    val mainBgColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = mainBgColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.hasPages) {
                val estimatedBytes = uiState.options.compressionProfile.estimateSizeBytes(uiState.pages)
                val currentPage = uiState.pages.getOrNull(pagerState.currentPage)

                EditorTopBar(
                    fileName = uiState.options.fileName,
                    pageCount = uiState.pageCount,
                    estimatedSizeBytes = estimatedBytes,
                    hasCurrentPage = currentPage != null,
                    onBackClick = { isDiscardDialogVisible = true },
                    onRenameClick = { isRenameDialogOpen = true },
                    onGridClick = { isPagesGridVisible = true },
                    onAddPagesClick = { captureSourceSheetTitle = "Add Pages" },
                    onRotateCurrentPage = { currentPage?.let { viewModel.rotatePage(it.id) } },
                    onRotateAllPages = { viewModel.rotateAllPages() },
                    onOptionsClick = { viewModel.showOptionsBottomSheet(true) },
                    onDeleteCurrentPage = { currentPage?.let { viewModel.removePage(it.id) } },
                    isComparing = isComparingOriginal,
                    onCompareChange = { isComparingOriginal = it },
                    canUndo = currentPage?.let { uiState.canUndo(it.id) } ?: false,
                    onUndoClick = { currentPage?.let { viewModel.undoPageEdit(it.id) } },
                    canRedo = currentPage?.let { uiState.canRedo(it.id) } ?: false,
                    onRedoClick = { currentPage?.let { viewModel.redoPageEdit(it.id) } }
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = mainBgColor
                    )
                )
            }
        },
        bottomBar = {
            if (!uiState.hasPages) {
                NavigationBar(
                    containerColor = mainBgColor,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEach { tab ->
                        val isSelected = tabPagerState.currentPage == tab.ordinal
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                coroutineScope.launch {
                                    tabPagerState.scrollToPage(tab.ordinal)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
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
                HorizontalPager(
                    state = tabPagerState,
                    beyondViewportPageCount = 0,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    when (tabs[pageIndex]) {
                        MainNavTab.HOME -> {
                            HomeScreen(
                                recentPdfs = recentPdfsUiState.recentPdfs,
                                onScanConvertClick = { captureSourceSheetTitle = "Scan & Convert to PDF" },
                                onGalleryClick = { launchers.launchPhotoPicker() },
                                onCameraClick = { launchers.launchCamera() },
                                onSeeAllToolsClick = {
                                    coroutineScope.launch {
                                        tabPagerState.scrollToPage(MainNavTab.TOOLS.ordinal)
                                    }
                                },
                                onSeeAllResultsClick = {
                                    coroutineScope.launch {
                                        tabPagerState.scrollToPage(MainNavTab.RESULTS.ordinal)
                                    }
                                },
                                onPdfClick = { viewModel.openPdfInViewer(it) },
                                onShareClick = { PdfDocumentSharer.share(context, it) },
                                onDeleteClick = { recentPdfsViewModel.deletePdf(it) },
                                onToggleFavorite = { recentPdfsViewModel.toggleFavorite(it) },
                                isRefreshing = recentPdfsUiState.isRefreshing,
                                onRefresh = { recentPdfsViewModel.refresh() }
                            )
                        }

                        MainNavTab.TOOLS -> {
                            ToolsScreen(
                                onPresetsClick = { isPresetManagerVisible = true },
                                onOpenPdfClick = { launchers.launchOpenPdf() }
                            )
                        }

                        MainNavTab.RESULTS -> {
                            ResultsScreen(
                                recentPdfs = recentPdfsUiState.recentPdfs,
                                categories = recentPdfsUiState.categories,
                                pinnedFolders = recentPdfsUiState.pinnedFolders,
                                onTogglePinFolder = { recentPdfsViewModel.togglePinFolder(it) },
                                onPdfClick = { viewModel.openPdfInViewer(it) },
                                onShareClick = { PdfDocumentSharer.share(context, it) },
                                onShareMultiple = { files -> PdfDocumentSharer.shareMultiple(context, files) },
                                onDeleteClick = { recentPdfsViewModel.deletePdf(it) },
                                onToggleFavorite = { recentPdfsViewModel.toggleFavorite(it) },
                                onAssignFolders = { file, folders -> recentPdfsViewModel.updatePdfFolders(file, folders) },
                                onAddCategory = { recentPdfsViewModel.addCategory(it) },
                                onRenameCategory = { oldName, newName -> recentPdfsViewModel.renameCategory(oldName, newName) },
                                onDeleteCategory = { recentPdfsViewModel.deleteCategory(it) },
                                onReorderPinnedFolders = { recentPdfsViewModel.setPinnedFolders(it) },
                                isLoading = recentPdfsUiState.isLoading,
                                isRefreshing = recentPdfsUiState.isRefreshing,
                                onRefresh = { recentPdfsViewModel.refresh() }
                            )
                        }

                        MainNavTab.SETTINGS -> {
                            SettingsScreen(
                                destinationFolderDisplayName = uiState.destinationFolderDisplayName,
                                onChangeDestinationFolder = { launchers.launchFolderPicker() },
                                defaultPageSize = uiState.options.pageSize,
                                onDefaultPageSizeChange = { viewModel.updateDefaultPageSize(it) },
                                defaultOrientation = uiState.options.orientation,
                                onDefaultOrientationChange = { viewModel.updateDefaultOrientation(it) },
                                defaultCompression = uiState.options.compressionProfile,
                                onDefaultCompressionChange = { viewModel.updateDefaultCompression(it) },
                                defaultFilter = uiState.defaultFilter,
                                onDefaultFilterChange = { viewModel.updateDefaultFilter(it) },
                                presetsCount = uiState.presets.size,
                                onNavigateToPresets = { isPresetManagerVisible = true }
                            )
                        }
                    }
                }
            } else {
                val estimatedBytes = uiState.options.compressionProfile.estimateSizeBytes(uiState.pages)
                val currentPage = uiState.pages.getOrNull(pagerState.currentPage)
                EditorWorkspace(
                    pages = uiState.pages,
                    pagerState = pagerState,
                    estimatedSizeBytes = estimatedBytes,
                    onMovePage = { sourceIndex, targetIndex -> viewModel.movePage(sourceIndex, targetIndex) },
                    onRemovePage = { viewModel.removePage(it) },
                    onFilterSelected = { pageId, filter -> viewModel.updatePageFilter(pageId, filter) },
                    onApplyFilterToAll = { filter -> viewModel.applyFilterToAll(filter) },
                    onAdjustmentChange = { pageId, contrast, brightness, sharpness ->
                        viewModel.updatePageAdjustment(pageId, contrast, brightness, sharpness)
                    },
                    onAdjustmentChangeFinished = { pageId ->
                        viewModel.commitPageAdjustment(pageId)
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
                    onPerspectiveQuadChange = { pageId, quad ->
                        viewModel.updatePagePerspectiveQuad(pageId, quad)
                    },
                    onApplyCropAndPerspective = { pageId, bounds, quad ->
                        viewModel.applyPageCropAndPerspective(pageId, bounds, quad)
                    },
                    onAutoDetectPerspective = { pageId ->
                        viewModel.autoDetectPagePerspective(pageId, context.contentResolver)
                    },
                    onResetPerspective = { pageId ->
                        viewModel.resetPagePerspectiveQuad(pageId)
                    },
                    onRotatePage = { pageId ->
                        viewModel.rotatePage(pageId)
                    },
                    onRotatePageLeft = { pageId ->
                        viewModel.rotatePageLeft(pageId)
                    },
                    onResetCrop = { pageId ->
                        viewModel.resetPageCrop(pageId)
                    },
                    canUndoAnnotation = currentPage?.let { uiState.canUndoAnnotation(it.id) } ?: false,
                    canRedoAnnotation = currentPage?.let { uiState.canRedoAnnotation(it.id) } ?: false,
                    onAddAnnotation = { pageId, annotation ->
                        viewModel.addPageAnnotation(pageId, annotation)
                    },
                    onUpdateAnnotation = { pageId, annotation ->
                        viewModel.updatePageAnnotation(pageId, annotation)
                    },
                    onRemoveAnnotation = { pageId, annotationId ->
                        viewModel.removePageAnnotation(pageId, annotationId)
                    },
                    onUndoAnnotation = { pageId ->
                        viewModel.undoPageAnnotation(pageId)
                    },
                    onRedoAnnotation = { pageId ->
                        viewModel.redoPageAnnotation(pageId)
                    },
                    onClearAnnotations = { pageId ->
                        viewModel.clearPageAnnotations(pageId)
                    },
                    onCancel = { isDiscardDialogVisible = true },
                    onOpenOptionsSheet = { viewModel.showOptionsBottomSheet(true) },
                    onAddPagesClick = { captureSourceSheetTitle = "Add Pages" },
                    isComparing = isComparingOriginal,
                    onCompareChange = { isComparingOriginal = it }
                )
            }
        }
    }

    MainScreenModals(
        uiState = uiState,
        recentPdfsUiState = recentPdfsUiState,
        viewModel = viewModel,
        recentPdfsViewModel = recentPdfsViewModel,
        launchers = launchers,
        pagerState = pagerState,
        bottomSheetState = bottomSheetState,
        isPagesGridVisible = isPagesGridVisible,
        onDismissPagesGrid = { isPagesGridVisible = false },
        isRenameDialogOpen = isRenameDialogOpen,
        onDismissRenameDialog = { isRenameDialogOpen = false },
        isDiscardDialogVisible = isDiscardDialogVisible,
        onDismissDiscardDialog = { isDiscardDialogVisible = false },
        captureSourceSheetTitle = captureSourceSheetTitle,
        onDismissCaptureSourceSheet = { captureSourceSheetTitle = null },
        onOpenPdfInViewer = { viewModel.openPdfInViewer(it) },
        onNavigateToResults = {
            coroutineScope.launch {
                tabPagerState.scrollToPage(MainNavTab.RESULTS.ordinal)
            }
        },
        context = context
    )
}
