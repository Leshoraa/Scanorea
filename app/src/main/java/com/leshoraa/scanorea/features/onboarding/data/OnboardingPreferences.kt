package com.leshoraa.scanorea.features.onboarding.data

import android.content.Context
import androidx.core.content.edit

/**
 * Stores whether the first-launch app guide has been completed.
 *
 * The version suffix lets a future guide refresh be shown once without
 * resetting unrelated user settings.
 */
class OnboardingPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isCompleted(): Boolean = prefs.getBoolean(KEY_COMPLETED, false)

    fun markCompleted() {
        prefs.edit { putBoolean(KEY_COMPLETED, true) }
    }

    private companion object {
        const val PREFS_NAME = "scanorea_onboarding_prefs"
        const val KEY_COMPLETED = "key_guide_completed_v1"
    }
}
