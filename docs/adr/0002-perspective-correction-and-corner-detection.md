# 2. 4-Corner Perspective Correction and Paper Corner Detection Architecture

## Status
Accepted

## Context
When users photograph physical documents, receipts, or notes with mobile cameras, the paper is rarely perfectly parallel to the camera sensor. This results in keystoning, perspective slant, and non-rectangular boundaries. To produce professional PDF scans, Scanorea requires:
- Rectification of four-corner quadrilaterals into flat, rectangular pages.
- Automatic detection of paper boundaries on camera photographs.
- Fine-grained interactive manual adjustment for imperfect edge detections.
- Strict avoidance of heavyweight external libraries (such as OpenCV, ML Kit, or C++ native binaries) to keep the application lightweight, fast, and completely offline.

## Decision
1. **Hardware-Accelerated Native Homography (`PerspectiveWarpCalculator`)**:
   - Utilize `android.graphics.Matrix.setPolyToPoly` to calculate the 3x3 projective transformation matrix directly from four arbitrary source vertices to rectangular destination bounds.
   - Render the warped image directly using `android.graphics.Canvas.drawBitmap` with bilinear filtering (`Paint.ANTI_ALIAS_FLAG` and `Paint.FILTER_BITMAP_FLAG`).
   - This executes natively on Android's graphics pipeline without requiring external native dependencies.

2. **Contrast-Gradient Paper Corner Detection (`DocumentCornerDetector`)**:
   - Subsample input images to a maximum bounding dimension of 360px via `ImageAnalyzer.decodeSampledBitmap` to ensure minimal memory allocation and deterministic execution speeds under 50ms.
   - Compute ITU-R BT.601 luminance values (`0.299 R + 0.587 G + 0.114 B`) across borders and center regions to establish background and foreground segmentation thresholds.
   - Project candidate document pixels along diagonal extrema (`x + y` and `x - y`) to resolve candidate corners.
   - If document contrast is insufficient (contrast delta < 15) or candidate area is outside reasonable ratios, fall back gracefully to a safe inset margin (`DocumentQuad.ofInsetMargin(0.04f)`).

3. **Immutable Convex Geometry Model (`DocumentQuad`)**:
   - Encapsulate normalized corner coordinates `[0.0f..1.0f]` in an immutable data class.
   - Verify polygon convexity via 2D cross-product sign consistency across all four edges to reject concave or self-intersecting (bowtie) polygons before rendering.
   - Calculate output bitmap dimensions dynamically based on Euclidean edge lengths to avoid aspect distortion.

4. **Direct-Touch Presentation Overlay (`EditorPerspectiveOverlay`)**:
   - Render interactive draggable corner pins with haptic feedback and distinct visual states.
   - Use `ClipOp.Difference` to darken the exterior scrim without re-allocating secondary bitmaps.
   - Keep the underlying pager preview fixed to default coordinates during active corner dragging to eliminate visual feedback loops.

## Alternatives Considered
- **OpenCV Android SDK**:
  - *Rejected*: Adds 20+ MB across ABI architectures (`arm64-v8a`, `armeabi-v7a`, `x86_64`), complicates Gradle build pipelines, and introduces unneeded complexity for standard quadrilateral homography.
- **ML Kit Document Scanner API**:
  - *Rejected*: Couples the application to Google Play Services, prevents offline-first use on non-GMS devices, and forces a closed, uncustomizable modal UI that breaks Scanorea's unified editor workflow.
- **RenderScript ScriptIntrinsic**:
  - *Rejected*: Deprecated since Android 12 with hardware support removed on newer chipsets.

## Consequences
- **Positive**:
  - Zero APK size increase; no external dependencies added.
  - Sub-50ms execution time on mobile devices with negligible memory footprint.
  - Complete offline support independent of Google Play Services.
  - Seamless integration into existing Coil preview and PDF generation pipelines.
- **Trade-offs**:
  - Low-contrast environments (e.g., white paper on an identical white desk) fall back to safe margins, requiring user manual pin adjustment.
