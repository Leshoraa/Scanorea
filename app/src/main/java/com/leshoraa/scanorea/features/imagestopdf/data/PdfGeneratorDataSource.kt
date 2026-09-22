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
                        paint = paint
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
        paint: Paint
    ) {
        if (isFitToImage) {
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            return
        }

        val printableWidth = (pageWidth - (margin * 2)).coerceAtLeast(1f)
        val printableHeight = (pageHeight - (margin * 2)).coerceAtLeast(1f)

        val scaleX = printableWidth / bitmap.width.toFloat()
        val scaleY = printableHeight / bitmap.height.toFloat()
        val scale = min(scaleX, scaleY)

        val destinationWidth = bitmap.width * scale
        val destinationHeight = bitmap.height * scale

        val left = margin + (printableWidth - destinationWidth) / 2f
        val top = margin + (printableHeight - destinationHeight) / 2f

        val destinationRect = RectF(left, top, left + destinationWidth, top + destinationHeight)
        canvas.drawBitmap(bitmap, null, destinationRect, paint)
    }
}
