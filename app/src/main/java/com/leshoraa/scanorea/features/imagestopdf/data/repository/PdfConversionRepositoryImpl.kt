package com.leshoraa.scanorea.features.imagestopdf.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.leshoraa.scanorea.core.common.ResourceResult
import com.leshoraa.scanorea.features.imagestopdf.data.PdfGeneratorDataSource
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ConversionProgress
import com.leshoraa.scanorea.features.imagestopdf.domain.model.ImagePage
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfConversionOptions
import com.leshoraa.scanorea.features.imagestopdf.domain.model.PdfConversionResult
import com.leshoraa.scanorea.features.imagestopdf.domain.repository.PdfConversionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.IOException

/**
 * Concrete implementation of [PdfConversionRepository].
 * Coordinates background PDF generation and manages secure URI exposure via FileProvider.
 */
class PdfConversionRepositoryImpl(
    private val context: Context,
    private val pdfGeneratorDataSource: PdfGeneratorDataSource
) : PdfConversionRepository {

    override fun convertImagesToPdf(
        pages: List<ImagePage>,
        options: PdfConversionOptions,
        destinationFolderUri: android.net.Uri?,
        onProgress: (ConversionProgress) -> Unit
    ): Flow<ResourceResult<PdfConversionResult>> = flow {
        emit(ResourceResult.Loading)

        try {
            val outputDirectory = File(context.filesDir, "pdfs")
            val generatedFile = pdfGeneratorDataSource.generatePdf(
                pages = pages,
                options = options,
                outputDirectory = outputDirectory,
                onProgress = onProgress
            )

            // If a custom destination folder was chosen via SAF, export a copy there
            var exportFailureMessage: String? = null
            if (destinationFolderUri != null) {
                try {
                    val treeDoc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, destinationFolderUri)
                    if (treeDoc != null && treeDoc.canWrite()) {
                        val exportedFile = treeDoc.createFile("application/pdf", generatedFile.name)
                        if (exportedFile != null) {
                            context.contentResolver.openOutputStream(exportedFile.uri)?.use { out ->
                                java.io.FileInputStream(generatedFile).use { input ->
                                    input.copyTo(out)
                                }
                            } ?: run {
                                exportFailureMessage = "Could not open stream for destination folder."
                            }
                        } else {
                            exportFailureMessage = "Could not create file in destination folder."
                        }
                    } else {
                        exportFailureMessage = "Destination folder is not writable or access was revoked."
                    }
                } catch (ce: kotlinx.coroutines.CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    exportFailureMessage = e.localizedMessage ?: "SAF write error"
                }
            } else {
                try {
                    exportToPublicScanoreaFolder(generatedFile)
                } catch (ce: kotlinx.coroutines.CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    android.util.Log.w("PdfConversionRepo", "Could not export to public Documents/Scanorea", e)
                }
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                generatedFile
            )

            val result = PdfConversionResult(
                file = generatedFile,
                contentUri = contentUri,
                pageCount = pages.size,
                fileSizeBytes = generatedFile.length(),
                exportFailureMessage = exportFailureMessage
            )

            emit(ResourceResult.Success(result))
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (oom: OutOfMemoryError) {
            emit(
                ResourceResult.Error(
                    exception = oom,
                    userMessage = "Insufficient device memory to process photos. Please select a lower quality in PDF options."
                )
            )
        } catch (ioe: IOException) {
            emit(
                ResourceResult.Error(
                    exception = ioe,
                    userMessage = "Failed to process PDF file: ${ioe.localizedMessage ?: "I/O Error"}"
                )
            )
        } catch (e: Exception) {
            emit(
                ResourceResult.Error(
                    exception = e,
                    userMessage = "An unexpected error occurred while converting images to PDF."
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    private fun exportToPublicScanoreaFolder(generatedFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, generatedFile.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/Scanorea")
            }
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = context.contentResolver.insert(collection, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    java.io.FileInputStream(generatedFile).use { input ->
                        input.copyTo(out)
                    }
                }
            }
        } else {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val scanoreaDir = File(docsDir, "Scanorea")
            if (!scanoreaDir.exists()) {
                scanoreaDir.mkdirs()
            }
            val targetFile = File(scanoreaDir, generatedFile.name)
            generatedFile.copyTo(targetFile, overwrite = true)
        }
    }
}
