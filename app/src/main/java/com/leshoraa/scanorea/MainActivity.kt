package com.leshoraa.scanorea

import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.content.IntentCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leshoraa.scanorea.app.ui.AppEntryAction
import com.leshoraa.scanorea.app.ui.AppEntryRequest
import com.leshoraa.scanorea.app.ui.MainScreen
import com.leshoraa.scanorea.core.designsystem.ScanoreaTheme
import com.leshoraa.scanorea.features.imagestopdf.ui.ImagesToPdfViewModel
import com.leshoraa.scanorea.features.onboarding.data.OnboardingPreferences
import com.leshoraa.scanorea.features.onboarding.ui.OnboardingScreen

/**
 * Entry point activity hosting the Scanorea Jetpack Compose UI.
 *
 * Dynamically observes system Monet and Xiaomi HyperOS palette overlay changes
 * so the dynamic theme color scheme updates immediately when the user changes wallpaper
 * or system accent colors in settings.
 */
class MainActivity : ComponentActivity() {

    private var lastThemePalette: String? = null
    private var themeObserver: ContentObserver? = null
    private var pendingEntry by mutableStateOf<AppEntryRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            pendingEntry = parseEntryIntent(intent)
        }

        lastThemePalette = getSystemThemePalette()
        registerThemeObserver()
        com.leshoraa.scanorea.features.recentpdfs.data.PdfThumbnailLoader.initCacheDir(cacheDir)

        setContent {
            ScanoreaTheme {
                val onboardingPreferences = remember { OnboardingPreferences(this) }
                var showGuide by rememberSaveable {
                    mutableStateOf(!onboardingPreferences.isCompleted())
                }

                if (showGuide) {
                    OnboardingScreen(
                        onFinish = {
                            onboardingPreferences.markCompleted()
                            showGuide = false
                        },
                        onSkip = {
                            onboardingPreferences.markCompleted()
                            showGuide = false
                        }
                    )
                } else {
                    val isDark = isSystemInDarkTheme()
                    val mainActivityBgColor = if (isDark) {
                        MaterialTheme.colorScheme.surfaceContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = mainActivityBgColor
                    ) {
                        val viewModel: ImagesToPdfViewModel = viewModel(
                            factory = ImagesToPdfViewModel.provideFactory(this)
                        )
                        val recentPdfsViewModel: com.leshoraa.scanorea.features.recentpdfs.ui.RecentPdfsViewModel = viewModel(
                            factory = com.leshoraa.scanorea.features.recentpdfs.ui.RecentPdfsViewModel.provideFactory(this)
                        )
                        MainScreen(
                            viewModel = viewModel,
                            recentPdfsViewModel = recentPdfsViewModel,
                            pendingEntry = pendingEntry,
                            onEntryConsumed = { pendingEntry = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseEntryIntent(intent)?.let { pendingEntry = it }
    }

    override fun onResume() {
        super.onResume()
        val currentPalette = getSystemThemePalette()
        if (lastThemePalette != null && lastThemePalette != currentPalette) {
            lastThemePalette = currentPalette
            recreate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        themeObserver?.let { observer ->
            try {
                contentResolver.unregisterContentObserver(observer)
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "Failed to unregister theme observer", e)
            }
        }
    }

    private fun getSystemThemePalette(): String? {
        return try {
            Settings.Secure.getString(contentResolver, "theme_customization_overlay_packages")
        } catch (e: Exception) {
            android.util.Log.d("MainActivity", "Theme overlay setting unavailable", e)
            null
        }
    }

    private fun registerThemeObserver() {
        try {
            val uri = Settings.Secure.getUriFor("theme_customization_overlay_packages") ?: return
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    val currentPalette = getSystemThemePalette()
                    if (currentPalette != null && currentPalette != lastThemePalette) {
                        lastThemePalette = currentPalette
                        recreate()
                    }
                }
            }
            themeObserver = observer
            contentResolver.registerContentObserver(uri, false, observer)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Failed to register theme observer", e)
        }
    }

    private fun parseEntryIntent(intent: Intent?): AppEntryRequest? {
        if (intent == null) return null
        return when (intent.action) {
            ACTION_SCAN_DOCUMENT -> AppEntryRequest(action = AppEntryAction.SCAN_DOCUMENT)
            ACTION_IMPORT_PHOTOS -> AppEntryRequest(action = AppEntryAction.IMPORT_PHOTOS)
            ACTION_OPEN_PDF -> AppEntryRequest(action = AppEntryAction.OPEN_PDF)
            Intent.ACTION_SEND -> parseSendIntent(intent)
            Intent.ACTION_SEND_MULTIPLE -> parseSendMultipleIntent(intent)
            Intent.ACTION_VIEW -> parseViewIntent(intent)
            else -> null
        }
    }

    private fun parseSendIntent(intent: Intent): AppEntryRequest? {
        val type = intent.type.orEmpty()
        val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            ?: return null
        return when {
            type.startsWith("image/") -> AppEntryRequest(imageUris = listOf(uri))
            type == "application/pdf" -> AppEntryRequest(pdfUri = uri)
            else -> null
        }
    }

    private fun parseSendMultipleIntent(intent: Intent): AppEntryRequest? {
        if (!intent.type.orEmpty().startsWith("image/")) return null

        val streamUris = IntentCompat.getParcelableArrayListExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java
        ).orEmpty()
        val clipUris = buildList {
            intent.clipData?.let { clip ->
                for (index in 0 until clip.itemCount) {
                    clip.getItemAt(index).uri?.let(::add)
                }
            }
        }
        val uris = (streamUris + clipUris).distinct()
        return if (uris.isNotEmpty()) AppEntryRequest(imageUris = uris) else null
    }

    private fun parseViewIntent(intent: Intent): AppEntryRequest? {
        val uri = intent.data ?: return null
        val type = intent.type.orEmpty()
        val isPdf = type.startsWith("application/pdf") ||
            uri.lastPathSegment?.endsWith(".pdf", ignoreCase = true) == true
        return if (isPdf) AppEntryRequest(pdfUri = uri) else null
    }

    companion object {
        const val ACTION_SCAN_DOCUMENT = "com.leshoraa.scanorea.action.SCAN_DOCUMENT"
        const val ACTION_IMPORT_PHOTOS = "com.leshoraa.scanorea.action.IMPORT_PHOTOS"
        const val ACTION_OPEN_PDF = "com.leshoraa.scanorea.action.OPEN_PDF"
    }
}
