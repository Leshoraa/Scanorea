package com.leshoraa.scanorea.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.leshoraa.scanorea.features.imagestopdf.ui.ImagesToPdfViewModel
import java.io.File

/**
 * Encapsulated system activity launchers for photo picking, camera capture,
 * folder tree selection, and document opening.
 */
class MainScreenLaunchers(
    val launchPhotoPicker: () -> Unit,
    val launchCamera: () -> Unit,
    val launchFolderPicker: () -> Unit,
    val launchOpenPdf: () -> Unit
)

@Composable
fun rememberMainScreenLaunchers(
    viewModel: ImagesToPdfViewModel,
    context: Context = LocalContext.current
): MainScreenLaunchers {
    var activeCameraCaptureUriString by rememberSaveable { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris, context.contentResolver)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uriString = activeCameraCaptureUriString
        val uri = uriString?.let { Uri.parse(it) }
        activeCameraCaptureUriString = null
        if (success && uri != null) {
            viewModel.addImages(listOf(uri), context.contentResolver)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                android.util.Log.w("MainScreenLaunchers", "Failed to obtain persistable permission for $uri", e)
            }
            val doc = DocumentFile.fromTreeUri(context, uri)
            val displayName = doc?.name ?: "Custom Folder"
            viewModel.setCustomDestinationFolder(uri, displayName)
            Toast.makeText(context, "Save location set to: $displayName", Toast.LENGTH_SHORT).show()
        }
    }

    val openPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.openPdfFromExternalUri(uri, context.contentResolver, context.cacheDir)
        }
    }

    fun openCamera() {
        try {
            val cachePdfs = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val cameraCaptureFile = File.createTempFile("scan_", ".jpg", cachePdfs)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cameraCaptureFile
            )
            activeCameraCaptureUriString = uri.toString()
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            android.util.Log.e("MainScreenLaunchers", "Cannot open camera", e)
            Toast.makeText(context, "Cannot open camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    return remember(viewModel, context) {
        MainScreenLaunchers(
            launchPhotoPicker = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            launchCamera = { openCamera() },
            launchFolderPicker = { folderPickerLauncher.launch(null) },
            launchOpenPdf = { openPdfLauncher.launch(arrayOf("application/pdf")) }
        )
    }
}
