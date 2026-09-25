# 3. Pinned Folders 2x2 Grid Navigation and Categorization Architecture

## Status
Accepted

## Context
In the previous design, document categories and folders were rendered in a single horizontally-scrolling `LazyRow` of filter chips at the top of `ResultsScreen`. As users created more folders, navigating them required tedious horizontal scrolling. Furthermore:
- Primary document folders lacked visual prominence and hierarchical structure.
- Finding specific categories became harder as folder count scaled.
- The primary "Convert to PDF" button contained an redundant document/pdf icon that cluttered the primary action label.
- Users requested a visual navigation layout inspired by Google Photos collections: a compact 2x2 grid displaying up to 3 pinned folders plus an "Other" entry that opens a comprehensive folder management sheet.

## Decision
1. **Google Photos Style 2x2 Grid Layout (`FolderGridSection`)**:
   - Provide a balanced 2x2 grid (two rows of two cards) positioned prominently in `ResultsScreen`.
   - Slot allocation:
     - Up to 3 pinned folder cards.
     - Exactly 1 "Other" card in the 4th position.
   - Default pinned folders out of the box (`Favorites`, `Work`, `Study`) to guarantee a complete, visually appealing 2x2 grid from first launch without empty slots.
   - When a folder card is tapped, the list filters by that category and displays an active filter banner with a clear button.
   - If the active filter matches one of the pinned cards, that card displays active primary container tonal styling (`0.dp` elevation).

2. **All Folders Modal Bottom Sheet (`AllFoldersBottomSheet`)**:
   - Tapping the "Other" card opens `AllFoldersBottomSheet`.
   - Displays all existing categories alongside "All Documents" and "Favorites".
   - Shows the live count of documents within each folder.
   - Provides pin/unpin toggles on each custom folder, strictly enforcing the maximum 3 pinned folder limit.
   - Provides options to create new folders, rename existing folders, and delete folders with cascading updates.

3. **Domain and Repository Persistence (`RecentPdfsRepository`)**:
   - Persist pinned folders as an ordered list in `SharedPreferences` via pure Kotlin JSON serialization (`serializeStringList` / `deserializeStringList`).
   - Pure Kotlin serialization avoids Android framework stub dependencies during plain JVM unit tests.
   - Expose atomic repository operations:
     - `getPinnedFolders(): List<String>`
     - `setPinnedFolders(folders: List<String>)`
     - `togglePinFolder(folder: String): Boolean`
   - Automatically reconcile pinned folders when folders are renamed or deleted to prevent orphaned references.

4. **Action Button Simplification**:
   - Remove redundant PDF/document icons from `EditorBottomActionBar` and `ConversionOptionsBottomSheet`.
   - The primary button focuses cleanly on the text label "Convert to PDF" for maximum legibility and reduced cognitive noise.

## Consequences
- **Positive**:
  - Eliminates endless horizontal scrolling for folder discovery.
  - Gives users instant 1-tap access to their 3 most frequent categories directly on the main screen.
  - Maintains a clean Material 3 Flat Design with `0.dp` elevation across all cards and bottom sheets.
  - Fully decoupled architecture with comprehensive unit tests for pinned folder manipulation and synchronization.
- **Trade-offs**:
  - Pinned folders are capped at 3 to preserve the clean 2x2 visual layout. Additional folders are accessed via the "Other" card.
