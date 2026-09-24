# 1. Architecture for Images to PDF Conversion and Jetpack Compose UI

## Status
Accepted

## Context
Scanorea requires a robust, privacy-respecting, and user-friendly mobile application to convert multiple user-selected photos into high-quality PDF documents. Key requirements and architectural constraints include:
- Memory safety and OOM prevention when handling multi-megapixel camera images (12MP - 108MP).
- Zero broad storage permissions (`READ_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE`) while supporting custom save locations.
- Seamless in-app PDF previewing without requiring external viewer dependencies or commercial SDKs.
- Document-scanner quality post-processing (black & white document thresholding, grayscale, contrast enhancement).
- Predictive auto-compression with upfront estimated file sizes.
- Adherence to Material You (Material 3 Dynamic Color) guidelines, edge-to-edge layouts, and strict deep-module separation.

## Decision
1. **Jetpack Compose & Material 3 (Material You)**:
   - Built the entire presentation layer with Jetpack Compose using Material 3 dynamic color theming (`dynamicLightColorScheme` / `dynamicDarkColorScheme`) supported on Android 12+ (API 31+), with fallback palettes for older versions.
   - Replaced multi-column grids with a dedicated single-image full-screen `HorizontalPager` Carousel workspace for intuitive document page scanning, per-page reordering, and individual page deletion.
   - Streamlined conversion flow: Tapping the floating "Convert to PDF" button directly summons the options sheet with a primary "Convert to PDF" execution trigger.

2. **Native Android Platform APIs (`PdfDocument` & `PdfRenderer`)**:
   - Chose native `android.graphics.pdf.PdfDocument` for raster-to-vector page drawing and `android.graphics.pdf.PdfRenderer` for native in-app document viewing.
   - Avoided heavy third-party libraries (such as iText, PDFBox, or PSPDFKit), eliminating external licensing costs and keeping the APK footprint under ~22 MB.
   - Redesigned PDF viewer with tight `8.dp` page spacing, realistic paper elevation, and a non-obstructive floating page indicator positioned on the right-edge scrollbar track (`1 / N`).

3. **ColorMatrix Canvas Document Filtering & Dynamic Adjustments**:
   - Implemented `ImageFilterType` using hardware-accelerated `ColorMatrixColorFilter` and `android.graphics.Canvas`.
   - Engineered a high-contrast document threshold matrix (`contrast = 2.2f`, `offset = -120f`) applied by default on image import to convert photo shadows and background noise into crisp black-and-white text.
   - Integrated per-page dynamic Sharpness/Contrast (`0.5x` - `2.5x`) and Brightness (`-50` to `+50`) controls rendered in real time via Compose `ColorFilter.colorMatrix` and rendered into the PDF via Canvas matrix transformations.

4. **Predictive Auto-Compression Engine**:
   - Structured `CompressionProfile` (`AUTO_BALANCED`, `SMALL_FILE`, `HIGH_QUALITY`, `MAXIMUM`) with bounded dimension scaling and JPEG compression quality factors.
   - Added real-time heuristic file size estimation (`estimateSizeBytes(pageCount)`) to inform users before initiating conversion.

5. **Privacy-Preserving Storage & Custom Location Export (SAF)**:
   - Used Android Photo Picker (`ActivityResultContracts.PickMultipleVisualMedia`) for photo selection.
   - Internal app storage (`context.filesDir/pdfs`) provides persistent sandboxed storage for recent documents without permissions.
   - Integrated Android Storage Access Framework (`ActivityResultContracts.OpenDocumentTree` and `DocumentFile.fromTreeUri`) allowing users to choose an external destination folder directly in the conversion options sheet.
   - Retained `CreateDocument` "Save As" functionality in the PDF viewer for ad-hoc export to Google Drive, SD card, or Downloads.

6. **Dynamic Presets & Date Token Engine**:
   - Implemented `PresetRepository` with `SharedPreferences` + JSON serialization to store named presets (paper size, orientation, compression, filename template).
   - Designed `TemplateDateEvaluator` to parse template tokens (`{DD:MM:YYYY}`, `{DD-MM-YYYY}`, `{YYYY-MM-DD}`, `{YYYY}`, `{MM}`, `{DD}`) into safe hyphenated dates (e.g. `21-09-2026`).

7. **Deep Module & Reactive State Boundaries**:
   - Domain models (`ImagePage`, `PdfConversionOptions`, `CompressionProfile`, `ConversionPreset`, `RecentPdf`) are immutable.
   - Repositories (`PdfConversionRepository`, `RecentPdfsRepository`, `PresetRepository`) encapsulate IO dispatching, subsampling, and error mapping into reactive Kotlin `Flow<ResourceResult<T>>` streams.

8. **Clean Architecture, Single Responsibility & Feature Decomposition**:
   - Deconstructed monolithic UI God files into modular, decoupled feature packages (`app/ui/MainScreen`, `features/home/`, `features/editor/`, `features/recentpdfs/components/`).
   - Split the full-screen photo editing workspace (`EditorWorkspace`) into single-responsibility components: `EditorCanvasPager`, `EditorSuggestionsPanel`, `EditorAdjustPanel`, `EditorCropPanel`, `EditorFiltersPanel`, `EditorCategoryTabBar`, and `EditorBottomActionBar`.
   - Purged dead components, non-English commentary, and decorative formatting across the codebase to adhere to rigorous software engineering standards.

## Alternatives Considered
- **Third-party PDF Rendering Engines (e.g., PdfiumAndroid, AndroidPdfViewer)**:
  - *Rejected*: Incurs native `.so` binary bloat (increasing APK size by 15-30 MB across ABIs), risks compatibility issues with modern Android 64-bit architectures, and introduces unmaintained dependencies.
- **External Intent Only for PDF Viewing**:
  - *Rejected*: Poor user experience when users do not have a default PDF viewer installed or do not want to switch away from Scanorea to preview generated scans.
- **RenderScript / OpenCV for Image Filtering**:
  - *Rejected*: RenderScript is deprecated as of Android 12; OpenCV adds 20+ MB native binaries. The Android `ColorMatrix` pipeline meets performance requirements while running natively via Android's Skia graphics engine.

## Consequences
- **Positive**:
  - Zero proprietary or heavy third-party PDF dependencies.
  - Zero runtime permission prompts required from users.
  - Low memory footprint via two-pass `BitmapFactory` subsampling and bitmap recycling.
  - Native in-app viewing with pinch-to-zoom, pan, page counter, and SAF export.
  - Dynamic Material You theming reflecting the user's system wallpaper accent.
- **Trade-offs**:
  - `PdfRenderer` requires seekable `ParcelFileDescriptor`, requiring temporary or cached files on disk rather than arbitrary network streams.
  - Advanced PDF features like digital signatures, vector font embedding, or editable form fields require dedicated engines outside the scope of photo-to-PDF scanning.
