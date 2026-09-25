package com.leshoraa.scanorea.features.recentpdfs.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests verifying [RecentPdfsRepository] pinned folders and folder life cycle.
 */
class RecentPdfsRepositoryTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var repository: RecentPdfsRepository

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        val dummyContext = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File("/tmp")
        }
        repository = RecentPdfsRepository(dummyContext, fakePrefs)
    }

    @Test
    fun getPinnedFolders_whenUninitialized_returnsDefaults() = runBlocking {
        val pinned = repository.getPinnedFolders()
        assertEquals(RecentPdfsRepository.DEFAULT_PINNED_FOLDERS, pinned)
        assertEquals(5, pinned.size)
        assertTrue(pinned.contains("Favorites"))
        assertTrue(pinned.contains("Work"))
        assertTrue(pinned.contains("Study"))
        assertTrue(pinned.contains("Personal"))
        assertTrue(pinned.contains("Projects"))
    }

    @Test
    fun setPinnedFolders_sanitizesAndEnforcesMaxFive() = runBlocking {
        val input = listOf("Work", "Study", "Personal", "Projects", "Archive", "Taxes")
        val result = repository.setPinnedFolders(input)
        assertTrue(result)

        val pinned = repository.getPinnedFolders()
        assertEquals(5, pinned.size)
        assertEquals(listOf("Work", "Study", "Personal", "Projects", "Archive"), pinned)
    }

    @Test
    fun togglePinFolder_whenNotPinnedAndUnderLimit_pinsFolder() = runBlocking {
        // Start with 4 pinned
        repository.setPinnedFolders(listOf("Favorites", "Work", "Study", "Personal"))
        val pinnedInitial = repository.getPinnedFolders()
        assertEquals(4, pinnedInitial.size)

        val result = repository.togglePinFolder("Projects")
        assertTrue(result)

        val pinnedAfter = repository.getPinnedFolders()
        assertEquals(5, pinnedAfter.size)
        assertTrue(pinnedAfter.contains("Projects"))
    }

    @Test
    fun togglePinFolder_whenAlreadyPinned_unpinsFolder() = runBlocking {
        repository.setPinnedFolders(listOf("Favorites", "Work", "Study"))

        val result = repository.togglePinFolder("Work")
        assertTrue(result)

        val pinnedAfter = repository.getPinnedFolders()
        assertEquals(2, pinnedAfter.size)
        assertFalse(pinnedAfter.contains("Work"))
        assertTrue(pinnedAfter.contains("Favorites"))
        assertTrue(pinnedAfter.contains("Study"))
    }

    @Test
    fun togglePinFolder_whenAtMaxLimit_rejectsNewPin() = runBlocking {
        repository.setPinnedFolders(listOf("Favorites", "Work", "Study", "Personal", "Projects"))

        val result = repository.togglePinFolder("Archive")
        assertFalse(result)

        val pinnedAfter = repository.getPinnedFolders()
        assertEquals(5, pinnedAfter.size)
        assertFalse(pinnedAfter.contains("Archive"))
    }

    @Test
    fun renameCategory_whenPinned_updatesPinnedFolderName() = runBlocking {
        repository.addCategory("Projects")
        repository.setPinnedFolders(listOf("Favorites", "Work", "Projects"))

        val renameResult = repository.renameCategory("Projects", "SecretProjects")
        assertTrue(renameResult)

        val pinnedAfter = repository.getPinnedFolders()
        assertTrue(pinnedAfter.contains("SecretProjects"))
        assertFalse(pinnedAfter.contains("Projects"))
    }

    @Test
    fun deleteCategory_whenPinned_removesFromPinnedFolders() = runBlocking {
        repository.addCategory("TempFolder")
        repository.setPinnedFolders(listOf("Favorites", "Work", "TempFolder"))

        val deleteResult = repository.deleteCategory("TempFolder")
        assertTrue(deleteResult)

        val pinnedAfter = repository.getPinnedFolders()
        assertEquals(2, pinnedAfter.size)
        assertFalse(pinnedAfter.contains("TempFolder"))
    }

    @Test
    fun deleteCategory_whenDeletingWork_removesWorkFromCategoriesAndPinned() = runBlocking {
        val deleteResult = repository.deleteCategory("Work")
        assertTrue(deleteResult)

        val categories = repository.getCategories()
        assertFalse(categories.contains("Work"))

        val pinned = repository.getPinnedFolders()
        assertFalse(pinned.contains("Work"))
    }

    /**
     * In-memory test double for Android [SharedPreferences].
     */
    private class FakeSharedPreferences : SharedPreferences {
        private val storage = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = storage

        override fun getString(key: String?, defValue: String?): String? {
            return storage[key] as? String ?: defValue
        }

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            return storage[key] as? MutableSet<String> ?: defValues
        }

        override fun getInt(key: String?, defValue: Int): Int = storage[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = storage[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = storage[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = storage[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = storage.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(storage)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val backingMap: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val tempChanges = mutableMapOf<String, Any?>()
            private val tempRemovals = mutableSetOf<String>()

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) tempChanges[key] = value
                return this
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null) tempChanges[key] = values
                return this
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) tempChanges[key] = value
                return this
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) tempChanges[key] = value
                return this
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) tempChanges[key] = value
                return this
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) tempChanges[key] = value
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) tempRemovals.add(key)
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                backingMap.clear()
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                tempRemovals.forEach { backingMap.remove(it) }
                backingMap.putAll(tempChanges)
                tempRemovals.clear()
                tempChanges.clear()
            }
        }
    }
}
