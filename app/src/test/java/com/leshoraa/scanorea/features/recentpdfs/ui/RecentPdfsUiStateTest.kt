package com.leshoraa.scanorea.features.recentpdfs.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying [RecentPdfsUiState] defaults and state immutability.
 */
class RecentPdfsUiStateTest {

    @Test
    fun defaultState_hasExpectedDefaults() {
        val state = RecentPdfsUiState()
        assertTrue(state.recentPdfs.isEmpty())
        assertTrue(state.categories.isEmpty())
        assertEquals(listOf("Favorites", "Work", "Study", "Personal", "Projects"), state.pinnedFolders)
        assertFalse(state.isLoading)
    }

    @Test
    fun copyState_updatesCorrectly() {
        val state = RecentPdfsUiState(
            categories = listOf("Work", "Personal"),
            pinnedFolders = listOf("Work"),
            isLoading = true
        )
        assertEquals(listOf("Work", "Personal"), state.categories)
        assertEquals(listOf("Work"), state.pinnedFolders)
        assertTrue(state.isLoading)
    }
}
