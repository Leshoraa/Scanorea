package com.leshoraa.scanorea.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level navigation tabs for the primary application navigation bar.
 */
enum class MainNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    TOOLS("Tools", Icons.Filled.GridView, Icons.Outlined.GridView),
    RESULTS("Results", Icons.Filled.Schedule, Icons.Outlined.Schedule),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}
