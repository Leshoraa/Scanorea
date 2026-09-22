# Scanorea

## Overview
Scanorea is a modern Android application for converting batches of photos into high-quality PDF documents quickly, safely, and efficiently. Built with Jetpack Compose and Material You (Material 3 Dynamic Color), Scanorea provides an in-app native PDF viewer, professional document scanning filters (such as high-contrast B&W thresholding), intelligent auto-compression profiles with predictive size estimations, and flexible Storage Access Framework (SAF) folder export—all while preserving user privacy and preventing Out-Of-Memory errors on high-resolution camera photos.

## Features
- **Flexible Photo Import**: Import multiple photos seamlessly via Android Photo Picker or directly capture pages with the camera without requiring runtime storage permissions.
- **Single-Image Carousel Workspace**: Swipe through pages one at a time using `HorizontalPager` with real-time hardware-accelerated preview, page reordering (`<-` / `->`), and individual page deletion.
- **Document Scanner Filters with Fine-Tuning**:
  - *Instant B&W on Import*: Automatically applies high-contrast black-and-white thresholding (`contrast = 2.2f`, `offset = -120f`) to remove paper shadows, yellowing, and ambient gradients.
  - *Per-Page Sharpness & Brightness*: Dedicated sliders for Sharpness/Contrast (0.5x - 2.5x) and Brightness (-50 to +50) with quick reset.
  - *Interactive Filter Chips*: Quick-switch chips for Original Color, B&W Document, Grayscale, Magic Color (Enhanced), and a "To All" batch action.
- **Conversion Presets with Automated Date Tokens**:
  - Save, load, and manage conversion presets (paper size, orientation, compression, name template).
  - Dynamic date token evaluation: templates like `Rendra_23.XX.XXXX_Aljabar_{DD:MM:YYYY}` automatically evaluate `{DD:MM:YYYY}` to today's date (e.g., `21-09-2026`).
- **Smart Auto-Compression Profiles**: Choose optimal file sizes with real-time estimated file sizes before conversion:
  - *Auto Balanced* (~250 KB/page): Recommended for general sharing and archiving.
  - *Small File* (~100 KB/page): Highly compressed for email attachments and messaging.
  - *High Quality* (~500 KB/page): Balanced detail for clear reading.
  - *Maximum / Original* (~1.5 MB/page): Preserves full pixel resolution.
- **Built-in Native PDF Viewer**:
  - Tight 8.dp page spacing with realistic paper drop shadows.
  - Interactive touch-draggable Fast-Scroller thumb and track with `.systemGestureExclusion()` to prevent edge back gesture conflicts.
  - Dynamic page-under-scrollbar detection: automatically updates the page indicator (`1 / N` -> `2 / N`) based on whichever paper sheet is currently intersecting the scrollbar thumb.
  - Smooth pinch-to-zoom and pan gestures using Android's native `android.graphics.pdf.PdfRenderer`.
- **Custom Save Location (SAF)**: Choose a custom destination folder via Storage Access Framework (`OpenDocumentTree`) in the conversion options sheet or use "Save As" in the viewer.
- **Recent Documents Dashboard**: Access, preview, share, and manage previously converted PDFs directly from the home screen.
- **Dynamic Material You Design**: Fully adapts to Android 12+ system wallpaper color accents (Monet engine) with support for Dark Theme and edge-to-edge system navigation.

## Architecture
Scanorea strictly adheres to the **Deep Module**, **Feature Boundary**, and **Clean Architecture** guidelines:
- **Presentation Layer (UI)**: Pure Jetpack Compose components organized by feature boundaries (`app/ui/`, `features/home/ui/`, `features/editor/ui/`, `features/pdfviewer/ui/`, `features/recentpdfs/ui/`, `features/tools/ui/`, `features/settings/ui/`).
- **Domain Layer**: Clean, pure Kotlin business models and repository contracts (`features/*/domain/`).
- **Data Layer**: Concrete data sources, repositories, and platform integrations (`features/*/data/`).
- **Core Layer**: Shared cross-cutting concerns (`core/common/`, `core/designsystem/`, `core/filter/`, `core/util/`).

## Project Structure
```
Scanorea/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/leshoraa/scanorea/
│   │   │   │   ├── app/
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   └── MainNavTab.kt
│   │   │   │   │   └── ui/
│   │   │   │   │       └── MainScreen.kt
│   │   │   │   ├── core/
│   │   │   │   │   ├── common/
│   │   │   │   │   │   └── ResourceResult.kt
│   │   │   │   │   ├── designsystem/
│   │   │   │   │   │   ├── Color.kt
│   │   │   │   │   │   ├── Theme.kt
│   │   │   │   │   │   └── Type.kt
│   │   │   │   │   ├── filter/
│   │   │   │   │   │   ├── ImageAnalyzer.kt
│   │   │   │   │   │   └── ImageFilterType.kt
│   │   │   │   │   └── util/
│   │   │   │   │       ├── DateTimeFormatter.kt
│   │   │   │   │       ├── FileSizeFormatter.kt
│   │   │   │   │       └── PdfShareUtil.kt
│   │   │   │   ├── features/
│   │   │   │   │   ├── editor/
│   │   │   │   │   │   ├── domain/model/
│   │   │   │   │   │   │   ├── AdjustmentTool.kt
│   │   │   │   │   │   │   └── EditorCategory.kt
│   │   │   │   │   │   └── ui/
│   │   │   │   │   │       ├── components/
│   │   │   │   │   │       │   ├── EditorAdjustPanel.kt
│   │   │   │   │   │       │   ├── EditorBottomActionBar.kt
│   │   │   │   │   │       │   ├── EditorCanvasPager.kt
│   │   │   │   │   │       │   ├── EditorCategoryTabBar.kt
│   │   │   │   │   │       │   ├── EditorCropPanel.kt
│   │   │   │   │   │       │   ├── EditorFiltersPanel.kt
│   │   │   │   │   │       │   ├── EditorRulerSlider.kt
│   │   │   │   │   │       │   └── EditorSuggestionsPanel.kt
│   │   │   │   │   │       └── EditorWorkspace.kt
│   │   │   │   │   ├── home/
│   │   │   │   │   │   └── ui/
│   │   │   │   │   │       ├── components/
│   │   │   │   │   │       │   ├── HomeHeroBanner.kt
│   │   │   │   │   │       │   └── QuickToolsSection.kt
│   │   │   │   │   │       └── HomeScreen.kt
│   │   │   │   │   ├── imagestopdf/
│   │   │   │   │   │   ├── data/
│   │   │   │   │   │   │   ├── ImageDecoderDataSource.kt
│   │   │   │   │   │   │   ├── PdfGeneratorDataSource.kt
│   │   │   │   │   │   │   └── repository/
│   │   │   │   │   │   │       └── PdfConversionRepositoryImpl.kt
│   │   │   │   │   │   ├── domain/
│   │   │   │   │   │   │   ├── model/
│   │   │   │   │   │   │   │   ├── CompressionProfile.kt
│   │   │   │   │   │   │   │   ├── ConversionProgress.kt
│   │   │   │   │   │   │   │   ├── ImagePage.kt
│   │   │   │   │   │   │   │   ├── PdfConversionOptions.kt
│   │   │   │   │   │   │   │   ├── PdfConversionResult.kt
│   │   │   │   │   │   │   │   ├── PdfPageOrientation.kt
│   │   │   │   │   │   │   │   ├── PdfPageSize.kt
│   │   │   │   │   │   │   │   └── PdfQuality.kt
│   │   │   │   │   │   │   └── repository/
│   │   │   │   │   │   │       └── PdfConversionRepository.kt
│   │   │   │   │   │   └── ui/
│   │   │   │   │   │       ├── components/
│   │   │   │   │   │       │   ├── ConversionOptionsBottomSheet.kt
│   │   │   │   │   │       │   ├── ConversionProgressDialog.kt
│   │   │   │   │   │       │   └── ConversionSuccessDialog.kt
│   │   │   │   │   │       ├── ImagesToPdfUiState.kt
│   │   │   │   │   │       └── ImagesToPdfViewModel.kt
│   │   │   │   │   ├── pdfviewer/
│   │   │   │   │   │   ├── data/
│   │   │   │   │   │   │   └── PdfRendererDataSource.kt
│   │   │   │   │   │   └── ui/
│   │   │   │   │   │       ├── components/
│   │   │   │   │   │       │   └── PdfFastScroller.kt
│   │   │   │   │   │       ├── util/
│   │   │   │   │   │       │   └── PdfScrollCalculator.kt
│   │   │   │   │   │       └── PdfViewerScreen.kt
│   │   │   │   │   ├── presets/
│   │   │   │   │   │   ├── data/
│   │   │   │   │   │   │   └── PresetRepository.kt
│   │   │   │   │   │   └── domain/
│   │   │   │   │   │       ├── TemplateDateEvaluator.kt
│   │   │   │   │   │       └── model/
│   │   │   │   │   │           └── ConversionPreset.kt
│   │   │   │   │   ├── recentpdfs/
│   │   │   │   │   │   ├── data/
│   │   │   │   │   │   │   └── RecentPdfsRepository.kt
│   │   │   │   │   │   ├── domain/model/
│   │   │   │   │   │   │   └── RecentPdf.kt
│   │   │   │   │   │   └── ui/
│   │   │   │   │   │       ├── components/
│   │   │   │   │   │       │   └── RecentPdfItemCard.kt
│   │   │   │   │   │       ├── RecentPdfsSection.kt
│   │   │   │   │   │       └── ResultsScreen.kt
│   │   │   │   │   ├── settings/
│   │   │   │   │   │   └── ui/
│   │   │   │   │   │       └── SettingsScreen.kt
│   │   │   │   │   └── tools/
│   │   │   │   │       └── ui/
│   │   │   │   │           └── ToolsScreen.kt
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/
│   │   │   │   └── xml/
│   │   │   │       └── file_paths.xml
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   │       └── java/com/leshoraa/scanorea/
│   └── build.gradle.kts
├── docs/
│   └── adr/
│       └── 0001-images-to-pdf-architecture.md
├── gradle/
│   └── libs.versions.toml
├── AI_RULES.md
└── README.md
```

## Requirements
- Android Studio Ladybug (2024.2) / Meerkat (2024.3) or newer.
- JDK 17 or newer (e.g., Android Studio bundled JBR).
- Android SDK 37 (compileSdk & targetSdk), minSdk 24 (Android 7.0 Nougat and above).

## Installation
Clone the repository and open it in Android Studio:
```bash
git clone <repository-url>
cd Scanorea
```

## Configuration
All file sharing paths are configured in `app/src/main/res/xml/file_paths.xml` and wired to Android's `FileProvider` in `AndroidManifest.xml`. No external API keys or cloud tokens are required; all processing runs 100% locally on the device.

## Usage
1. Launch Scanorea on your device.
2. Tap **Add Photos** or **Camera** to import images into your document workspace.
3. Every page automatically defaults to high-contrast **B&W** mode. Swipe across pages in the Carousel workspace, reorder pages with `<-` / `->`, or delete individual pages.
4. Fine-tune contrast/sharpness and brightness with sliders, or switch filters (Color, B&W, Gray, Enhanced).
5. Tap the floating **Convert to PDF** button.
6. In the conversion options sheet, pick or save a Preset (with automatic date tokens like `{DD:MM:YYYY}`), choose paper size, compression profile, and choose a custom destination folder via Storage Access Framework (`OpenDocumentTree`).
7. Tap **Convert to PDF** to create the document.
8. Preview pages in the native viewer with tight 8.dp page gaps and a scrollbar counter, or share via the system share sheet.

## Development
This project uses Kotlin 2.0.21 and Jetpack Compose with Gradle 9.5 and Android Gradle Plugin 9.3.3.

Compile Kotlin code:
```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew compileDebugKotlin
```

## Testing
Run the local unit test suite:
```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest
```

## Build
Generate a debug APK:
```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
```
The APK artifact will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Deployment
Install directly onto a connected physical Android device or emulator via ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Launch the main activity:
```bash
adb shell am start -n com.leshoraa.scanorea/.MainActivity
```

## Troubleshooting
- **High memory usage on low-RAM devices**: Choose the *Small File* or *Auto Balanced* compression profile to constrain maximum bitmap dimensions during two-pass decoding.
- **ColorMatrix filter appearance**: For dark shadows on scanned physical documents, ensure the page has good initial lighting; the *B&W Doc* filter applies a high-contrast offset (`-120f`) and contrast multiplier (`2.2f`) to cleanly separate ink from paper.

## Known Limitations
- Password encryption and digital signature embedding are not supported because native `android.graphics.pdf.PdfDocument` is focused on raster-to-vector page output.
- Input formats are restricted to formats supported by Android's `BitmapFactory` (JPEG, PNG, WEBP, HEIC).

## Architecture Decisions
- [ADR 0001: Architecture for Images to PDF Conversion and Jetpack Compose UI](docs/adr/0001-images-to-pdf-architecture.md)
