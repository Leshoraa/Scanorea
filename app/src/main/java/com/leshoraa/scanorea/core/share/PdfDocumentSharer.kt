package com.leshoraa.scanorea.core.share

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
}
