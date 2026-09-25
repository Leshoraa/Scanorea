# ADR 0004: Folder Lifecycle, Utility Restructuring, and ViewModel Boundary Decoupling

## Context
During feature expansion for 2x2 folder navigation and document management in the Results tab:
1. Deleting custom folders (such as "Work") failed to complete when initiated from the All Folders bottom sheet. The bottom sheet window remained active and visually obscured the confirmation dialog. Furthermore, desynchronization between categories and pinned folders in persistent storage caused silent failures or default folder resurrection.
2. `ImagesToPdfViewModel` grew into an overly broad class (over 740 lines) that coupled image scanning/conversion concerns with recent document querying, favorite toggling, category management, and folder pinning.
3. Utility packages `core.util` and `pdfviewer.ui.util` violated engineering rules against generic utility dumping grounds.

## Decisions
1. **Folder Lifecycle and Persistence Robustness**:
   - Refactored `RecentPdfsRepository.deleteCategory(category)` to independently and cleanly remove the target category from both `categories` and `pinnedFolders`, clean associated metadata, and commit changes using idiomatic `prefs.edit { ... }`.
   - Prevented default folder resurrection: `getCategories()` and `getPinnedFolders()` now only seed default values when preference keys are completely absent (`null`), never when explicitly emptied by the user.
   - Enforced modal dismiss in `ResultsScreen`: When invoking `onDeleteFolder` or `onRenameFolder` from `AllFoldersBottomSheet`, the sheet is dismissed (`isAllFoldersSheetVisible = false`) before presenting the dialog, ensuring clear visibility.
   - Enlarged `FolderGridSection` options button touch target to 36 dp with a 20 dp icon.

2. **ViewModel Boundary Decoupling**:
   - Created dedicated `RecentPdfsViewModel` under `features/recentpdfs/ui/RecentPdfsViewModel.kt`.
   - Moved all recent PDF querying, favorite toggles, category additions, renames, deletions, and folder pinning into `RecentPdfsViewModel`.
   - Stripped document management responsibilities from `ImagesToPdfViewModel`, restoring its focus strictly to image capture, editor workspace, and PDF conversion pipeline.
   - Wired `RecentPdfsViewModel` into `MainScreen` and `MainActivity`.

3. **Semantic Package Restructuring**:
   - Replaced `core.util` by moving `DateTimeFormatter` and `FileSizeFormatter` into `core.format`, and `PdfDocumentSharer` into `core.share`.
   - Replaced `pdfviewer.ui.util` by relocating `PdfScrollCalculator` to `pdfviewer.domain.PdfScrollCalculator`.
   - Updated all calling code and corresponding unit tests.

4. **Boolean Naming Compliance**:
   - Renamed flags across components to follow Rule 12 (`isOptionsMenuVisible`, `isSortDescending`, `isCreateFolderDialogVisible`, `isSavePresetDialogVisible`, `isPresetsDropdownExpanded`, `isEditorMenuVisible`, `isDiscardDialogVisible`).

## Consequences
- Folder creation, rename, and deletion now work reliably across both the grid and all-folders sheet.
- Architecture adheres to single responsibility and deep module principles.
- Codebase structure is clean, predictable, and fully verified by unit tests.
