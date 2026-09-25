package com.leshoraa.scanorea

import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leshoraa.scanorea.app.ui.MainScreen
import com.leshoraa.scanorea.core.designsystem.ScanoreaTheme
import com.leshoraa.scanorea.features.imagestopdf.ui.ImagesToPdfViewModel

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lastThemePalette = getSystemThemePalette()
        registerThemeObserver()

        setContent {
            ScanoreaTheme {
                val viewModel: ImagesToPdfViewModel = viewModel(
                    factory = ImagesToPdfViewModel.provideFactory(this)
                )
                val recentPdfsViewModel: com.leshoraa.scanorea.features.recentpdfs.ui.RecentPdfsViewModel = viewModel(
                    factory = com.leshoraa.scanorea.features.recentpdfs.ui.RecentPdfsViewModel.provideFactory(this)
                )
                MainScreen(
                    viewModel = viewModel,
                    recentPdfsViewModel = recentPdfsViewModel
                )
            }
        }
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
}