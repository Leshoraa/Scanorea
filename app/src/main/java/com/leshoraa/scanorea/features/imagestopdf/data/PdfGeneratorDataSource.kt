package com.leshoraa.scanorea.features.imagestopdf.data

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ConversionProgress
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfConversionOptions
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageOrientation
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfPageSize
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min

/**
 * Generates standard PDF documents from decoded bitmaps using the platform PdfDocument API.
 */
class PdfGeneratorDataSource(
    private val imageDecoderDataSource: ImageDecoderDataSource
) {

    /**
     * Renders images into a PDF file and writes it to [outputDirectory].
     */
    fun generatePdf(
        pages: List<ImagePage>,
        options: PdfConversionOptions,
        outputDirectory: File,
        onProgress: (ConversionProgress) -> Unit
    ): File {
        if (pages.isEmpty()) {
            throw IllegalArgumentException("Cannot generate PDF from an empty page list.")
        }

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val destinationFile = File(outputDirectory, options.sanitizedFileName())
        val pdfDocument = PdfDocument()

        try {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            pages.forEachIndexed { index, imagePage ->
                val pageIndex = index + 1
                val bitmap = imageDecoderDataSource.decodeBitmap(
                    uri = imagePage.uri,
                    maxDimensionPixels = options.compressionProfile.maxDimensionPixels,
                    filter = imagePage.filter,
                    contrast = imagePage.contrast,
                    brightness = imagePage.brightness,
                    rotationDegrees = imagePage.rotationDegrees,
                    cropBounds = imagePage.cropBounds
                )

                try {
                    val (pageWidth, pageHeight) = resolvePageDimensions(bitmap, options)
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
                    val page = pdfDocument.startPage(pageInfo)

                    renderBitmapToCanvas(
                        canvas = page.canvas,
                        bitmap = bitmap,
                        pageWidth = pageWidth,
                        pageHeight = pageHeight,
                        margin = options.marginPoints,
                        isFitToImage = options.pageSize == PdfPageSize.FIT_TO_IMAGE,
                        paint = paint,
                        annotations = imagePage.annotations
                    )

                    pdfDocument.finishPage(page)
                } finally {
                    bitmap.recycle()
                }

                onProgress(ConversionProgress(currentPageIndex = pageIndex, totalPages = pages.size))
            }

            FileOutputStream(destinationFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }

            return destinationFile
        } catch (e: Exception) {
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            throw IOException("PDF generation failed: ${e.message}", e)
        } finally {
            pdfDocument.close()
        }
    }

    private fun resolvePageDimensions(bitmap: Bitmap, options: PdfConversionOptions): Pair<Int, Int> {
        if (options.pageSize == PdfPageSize.FIT_TO_IMAGE) {
            return Pair(bitmap.width, bitmap.height)
        }

        var width = options.pageSize.defaultWidthPoints
        var height = options.pageSize.defaultHeightPoints

        when (options.orientation) {
            PdfPageOrientation.PORTRAIT -> {
                if (width > height) {
                    val temp = width
                    width = height
                    height = temp
                }
            }
            PdfPageOrientation.LANDSCAPE -> {
                if (width < height) {
                    val temp = width
                    width = height
                    height = temp
                }
            }
            PdfPageOrientation.AUTO -> {
                val isImageLandscape = bitmap.width > bitmap.height
                val isPageLandscape = width > height

                if (isImageLandscape != isPageLandscape) {
                    val temp = width
                    width = height
                    height = temp
                }
            }
        }

        return Pair(width, height)
    }

    private fun renderBitmapToCanvas(
        canvas: android.graphics.Canvas,
        bitmap: Bitmap,
        pageWidth: Int,
        pageHeight: Int,
        margin: Float,
        isFitToImage: Boolean,
        paint: Paint,
        annotations: List<com.leshoraa.scanorea.features.editor.domain.model.PageAnnotation> = emptyList()
    ) {
        val destinationRect: RectF
        if (isFitToImage) {
            destinationRect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        } else {
            val printableWidth = (pageWidth - (margin * 2)).coerceAtLeast(1f)
            val printableHeight = (pageHeight - (margin * 2)).coerceAtLeast(1f)

            val scaleX = printableWidth / bitmap.width.toFloat()
            val scaleY = printableHeight / bitmap.height.toFloat()
            val scale = min(scaleX, scaleY)

            val destinationWidth = bitmap.width * scale
            val destinationHeight = bitmap.height * scale

            val left = margin + (printableWidth - destinationWidth) / 2f
            val top = margin + (printableHeight - destinationHeight) / 2f

            destinationRect = RectF(left, top, left + destinationWidth, top + destinationHeight)
            canvas.drawBitmap(bitmap, null, destinationRect, paint)
        }

        if (annotations.isNotEmpty()) {
            renderAnnotations(canvas, destinationRect, annotations)
        }
    }

    private fun renderAnnotations(
        canvas: android.graphics.Canvas,
        destinationRect: RectF,
        annotations: List<com.leshoraa.scanorea.features.editor.domain.model.PageAnnotation>
    ) {
        val docW = destinationRect.width()
        val docH = destinationRect.height()
        if (docW <= 0f || docH <= 0f) return

        val scaleFactor = (docW / 360f).coerceIn(0.5f, 5f)

        for (annotation in annotations) {
            when (annotation) {
                is com.leshoraa.scanorea.features.editor.domain.model.PageAnnotation.FreehandPath -> {
                    val strokePx = (annotation.strokeWidth * scaleFactor).coerceAtLeast(1.5f)
                    val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = annotation.color.toInt()
                        style = Paint.Style.STROKE
                        strokeWidth = strokePx
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                    if (annotation.points.size > 1) {
                        val path = android.graphics.Path()
                        val first = annotation.points.first()
                        path.moveTo(destinationRect.left + first.x * docW, destinationRect.top + first.y * docH)
                        for (i in 1 until annotation.points.size) {
                            val p = annotation.points[i]
                            path.lineTo(destinationRect.left + p.x * docW, destinationRect.top + p.y * docH)
                        }
                        canvas.drawPath(path, pathPaint)
                    } else if (annotation.points.size == 1) {
                        val p = annotation.points.first()
                        pathPaint.style = Paint.Style.FILL
                        canvas.drawCircle(
                            destinationRect.left + p.x * docW,
                            destinationRect.top + p.y * docH,
                            strokePx / 2f,
                            pathPaint
                        )
                    }
                }

                is com.leshoraa.scanorea.features.editor.domain.model.PageAnnotation.RectBox -> {
                    val l = destinationRect.left + kotlin.math.min(annotation.left, annotation.right) * docW
                    val t = destinationRect.top + kotlin.math.min(annotation.top, annotation.bottom) * docH
                    val r = destinationRect.left + kotlin.math.max(annotation.left, annotation.right) * docW
                    val b = destinationRect.top + kotlin.math.max(annotation.top, annotation.bottom) * docH

                    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        val baseColor = annotation.color.toInt()
                        val alphaInt = (annotation.alpha * 255f).toInt().coerceIn(0, 255)
                        color = android.graphics.Color.argb(
                            alphaInt,
                            android.graphics.Color.red(baseColor),
                            android.graphics.Color.green(baseColor),
                            android.graphics.Color.blue(baseColor)
                        )
                        style = if (annotation.isFilled) Paint.Style.FILL else Paint.Style.STROKE
                        if (!annotation.isFilled) {
                            strokeWidth = (annotation.strokeWidth * scaleFactor).coerceAtLeast(1.5f)
                        }
                    }
                    canvas.drawRect(l, t, r, b, boxPaint)
                }

                is com.leshoraa.scanorea.features.editor.domain.model.PageAnnotation.OvalShape -> {
                    val l = destinationRect.left + kotlin.math.min(annotation.left, annotation.right) * docW
                    val t = destinationRect.top + kotlin.math.min(annotation.top, annotation.bottom) * docH
                    val r = destinationRect.left + kotlin.math.max(annotation.left, annotation.right) * docW
                    val b = destinationRect.top + kotlin.math.max(annotation.top, annotation.bottom) * docH

                    val ovalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = annotation.color.toInt()
                        style = Paint.Style.STROKE
                        strokeWidth = (annotation.strokeWidth * scaleFactor).coerceAtLeast(1.5f)
                    }
                    canvas.drawOval(RectF(l, t, r, b), ovalPaint)
                }

                is com.leshoraa.scanorea.features.editor.domain.model.PageAnnotation.ArrowLine -> {
                    val sx = destinationRect.left + annotation.startX * docW
                    val sy = destinationRect.top + annotation.startY * docH
                    val ex = destinationRect.left + annotation.endX * docW
                    val ey = destinationRect.top + annotation.endY * docH
                    val strokePx = (annotation.strokeWidth * scaleFactor).coerceAtLeast(2f)

                    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = annotation.color.toInt()
                        style = Paint.Style.STROKE
                        strokeWidth = strokePx
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                    canvas.drawLine(sx, sy, ex, ey, linePaint)

                    // Draw Arrowhead
                    val angle = kotlin.math.atan2((ey - sy).toDouble(), (ex - sx).toDouble())
                    val arrowLen = (strokePx * 3.8).coerceIn(12.0 * (scaleFactor / 1.5), 48.0 * (scaleFactor / 1.5))
                    val arrowAngle = Math.PI / 6.0
                    val x1 = (ex - arrowLen * kotlin.math.cos(angle - arrowAngle)).toFloat()
                    val y1 = (ey - arrowLen * kotlin.math.sin(angle - arrowAngle)).toFloat()
                    val x2 = (ex - arrowLen * kotlin.math.cos(angle + arrowAngle)).toFloat()
                    val y2 = (ey - arrowLen * kotlin.math.sin(angle + arrowAngle)).toFloat()

                    canvas.drawLine(ex, ey, x1, y1, linePaint)
                    canvas.drawLine(ex, ey, x2, y2, linePaint)
                }
            }
        }
    }
}
