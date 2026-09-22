package com.leshoraa.scanorea.features.recentpdfs.data

import android.content.Context
import com.leshoraa.scanorea.features.recentpdfs.domain.model.RecentPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository responsible for discovering and managing previously generated PDF documents.
 */
class RecentPdfsRepository(private val context: Context) {

    suspend fun getRecentPdfs(): List<RecentPdf> = withContext(Dispatchers.IO) {
        val pdfDir = File(context.filesDir, "pdfs")
        if (!pdfDir.exists() || !pdfDir.isDirectory) {
            return@withContext emptyList()
        }

        pdfDir.listFiles { file ->
            file.isFile && file.name.endsWith(".pdf", ignoreCase = true)
        }?.map { file ->
            RecentPdf(
                file = file,
                name = file.name,
                sizeBytes = file.length(),
                lastModifiedMillis = file.lastModified()
            )
        }?.sortedByDescending { it.lastModifiedMillis } ?: emptyList()
    }

    suspend fun deletePdf(file: File): Boolean = withContext(Dispatchers.IO) {
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}
