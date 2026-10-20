package com.leshoraa.scanorea.core.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Dispatches system intent chooser to share generated PDF documents.
 */
object PdfDocumentSharer {

    private const val TAG = "PdfDocumentSharer"

    /**
     * Shares the given PDF [file] via Android's ACTION_SEND intent chooser.
     */
    fun share(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch share intent for ${file.name}", e)
            Toast.makeText(context, "Cannot share PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares several generated PDF documents in one system chooser.
     */
    fun shareMultiple(context: Context, files: List<File>) {
        val distinctFiles = files.distinctBy { it.absolutePath }
        if (distinctFiles.isEmpty()) return
        if (distinctFiles.size == 1) {
            share(context, distinctFiles.first())
            return
        }

        try {
            val uris = distinctFiles.map { file ->
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }
            val documentsClip = ClipData.newUri(
                context.contentResolver,
                "PDF documents",
                uris.first()
            ).apply {
                uris.drop(1).forEach { uri ->
                    addItem(ClipData.Item(uri))
                }
            }
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "application/pdf"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                clipData = documentsClip
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share PDFs"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch multi-share intent for ${distinctFiles.size} PDFs", e)
            Toast.makeText(context, "Cannot share PDFs: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
