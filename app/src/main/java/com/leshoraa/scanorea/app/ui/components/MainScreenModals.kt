package com.leshoraa.scanorea.app.ui.components

import android.content.Context
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.leshoraa.scanorea.core.share.PdfDocumentSharer
import com.leshoraa.scanorea.features.editor.ui.components.DiscardChangesDialog
import com.leshoraa.scanorea.features.editor.ui.components.EditorPagesGridDialog
import com.leshoraa.scanorea.features.editor.ui.components.RenameDocumentDialog
import com.leshoraa.scanorea.features.imagestopdf.ui.ImagesToPdfUiState
import com.leshoraa.scanorea.features.imagestopdf.ui.ImagesToPdfViewModel
import com.leshoraa.scanorea.features.imagestopdf.ui.components.CaptureSourceBottomSheet
import com.leshoraa.scanorea.features.imagestopdf.ui.components.ConversionOptionsBottomSheet
import com.leshoraa.scanorea.features.imagestopdf.ui.components.ConversionProgressDialog
import com.leshoraa.scanorea.features.imagestopdf.ui.components.ConversionSuccessDialog
import com.leshoraa.scanorea.features.recentpdfs.ui.RecentPdfsUiState
import com.leshoraa.scanorea.features.recentpdfs.ui.RecentPdfsViewModel
import kotlinx.coroutines.launch
import java.io.File

/**
 * Encapsulated dialog and bottom sheet overlay host for the main application shell.
 * Isolates presentation lifecycles and modal visibility logic from [MainScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenModals(
    uiState: ImagesToPdfUiState,
    recentPdfsUiState: RecentPdfsUiState,
    viewModel: ImagesToPdfViewModel,
    recentPdfsViewModel: RecentPdfsViewModel,
    launchers: MainScreenLaunchers,
    pagerState: PagerState,
    bottomSheetState: SheetState,
    isPagesGridVisible: Boolean,
    onDismissPagesGrid: () -> Unit,
    isRenameDialogOpen: Boolean,
    onDismissRenameDialog: () -> Unit,
    isDiscardDialogVisible: Boolean,
    onDismissDiscardDialog: () -> Unit,
    captureSourceSheetTitle: String?,
    onDismissCaptureSourceSheet: () -> Unit,
    onOpenPdfInViewer: (File) -> Unit,
    onNavigateToResults: () -> Unit,
    context: Context
) {
    val coroutineScope = rememberCoroutineScope()

    // 1. Modal Bottom Sheet for Pages Grid Overview
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
            onAddPagesClick = { launchers.launchPhotoPicker() },
            onDismissRequest = onDismissPagesGrid
        )
    }

    // 2. Document Rename Dialog
    if (isRenameDialogOpen) {
        RenameDocumentDialog(
            currentFileName = uiState.options.fileName,
            presets = uiState.presets,
            onSave = { finalName, preset ->
                preset?.let { viewModel.applyPreset(it) }
                viewModel.updateFileName(finalName)
                onDismissRenameDialog()
            },
            onSaveNewPreset = { name, template ->
                viewModel.saveNewPreset(name, template)
            },
            onDeletePreset = { presetId ->
                viewModel.deletePreset(presetId)
            },
            onDismiss = onDismissRenameDialog
        )
    }

    // 3. Modal Bottom Sheet for Conversion Options, Presets, and Save Location
    if (uiState.isOptionsBottomSheetVisible) {
        ConversionOptionsBottomSheet(
            options = uiState.options,
            pageCount = uiState.pageCount,
            pages = uiState.pages,
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
            onChangeFolderClick = { launchers.launchFolderPicker() },
            onConvertClick = {
                viewModel.showOptionsBottomSheet(false)
                viewModel.convertImagesToPdf()
            },
            categories = recentPdfsUiState.categories,
            selectedFolders = uiState.selectedFolders,
            onToggleFolder = { viewModel.toggleSelectedFolder(it) },
            onCreateNewFolder = {
                recentPdfsViewModel.addCategory(it)
                viewModel.createNewFolder(it)
            }
        )
    }

    // 4. Progress Dialog during PDF generation
    if (uiState.isConverting) {
        ConversionProgressDialog(
            progress = uiState.conversionProgress,
            onCancel = { viewModel.cancelConversion() }
        )
    }

    // 5. Success Dialog on conversion completion
    val successResult = uiState.conversionResult
    if (successResult != null) {
        val assignedFolders = recentPdfsUiState.recentPdfs.find { it.name == successResult.file.name }?.folders ?: uiState.selectedFolders
        ConversionSuccessDialog(
            result = successResult,
            assignedFolders = assignedFolders,
            onOpenPdf = {
                val file = successResult.file
                viewModel.dismissResultDialog()
                viewModel.clearAllPages()
                onNavigateToResults()
                recentPdfsViewModel.loadRecentPdfs()
                onOpenPdfInViewer(file)
            },
            onSharePdf = {
                PdfDocumentSharer.share(context, successResult.file)
            },
            onKeepInEditor = {
                viewModel.dismissResultDialog()
                recentPdfsViewModel.loadRecentPdfs()
            },
            onDismiss = {
                viewModel.dismissResultDialog()
                viewModel.clearAllPages()
                onNavigateToResults()
                recentPdfsViewModel.loadRecentPdfs()
            }
        )
    }

    // 6. Confirmation dialog before discarding unsaved edits/scans
    if (isDiscardDialogVisible) {
        DiscardChangesDialog(
            onConfirmDiscard = {
                onDismissDiscardDialog()
                viewModel.clearAllPages()
            },
            onDismiss = onDismissDiscardDialog
        )
    }

    // 7. Source picker for camera or photo gallery capture
    captureSourceSheetTitle?.let { title ->
        CaptureSourceBottomSheet(
            title = title,
            onCameraClick = {
                onDismissCaptureSourceSheet()
                launchers.launchCamera()
            },
            onGalleryClick = {
                onDismissCaptureSourceSheet()
                launchers.launchPhotoPicker()
            },
            onDismiss = onDismissCaptureSourceSheet
        )
    }
}
