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
                MainScreen(viewModel = viewModel)
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
        themeObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (_: Exception) {}
        }
    }

    private fun getSystemThemePalette(): String? {
        return try {
            Settings.Secure.getString(contentResolver, "theme_customization_overlay_packages")
        } catch (_: Exception) {
            null
        }
    }

    private fun registerThemeObserver() {
        try {
            val uri = Settings.Secure.getUriFor("theme_customization_overlay_packages") ?: return
            themeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    val currentPalette = getSystemThemePalette()
                    if (currentPalette != null && currentPalette != lastThemePalette) {
                        lastThemePalette = currentPalette
                        recreate()
                    }
                }
            }
            contentResolver.registerContentObserver(uri, false, themeObserver!!)
        } catch (_: Exception) {}
    }
}