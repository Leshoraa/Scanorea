package com.leshoraa.scanorea.features.recentpdfs.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [RecentPdfsDao] running against an in-memory Room database.
 *
 * These exercise the production persistence path (Room), which the existing JVM tests
 * could not reach because they use the SharedPreferences fallback.
 *
 * Run on a physical device with:
 *   ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class RecentPdfsDaoTest {

    private lateinit var database: ScanoreaDatabase
    private lateinit var dao: RecentPdfsDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ScanoreaDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        dao = database.recentPdfsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertPdf_thenGetAllPdfs_returnsInsertedRow() = runBlocking {
        dao.upsertPdf(sampleEntity(path = "/files/pdfs/a.pdf", name = "a.pdf"))

        val all = dao.getAllPdfs()

        assertEquals(1, all.size)
        assertEquals("a.pdf", all.first().fileName)
    }

    @Test
    fun upsertPdf_whenSamePathTwice_replacesInsteadOfDuplicating() = runBlocking {
        dao.upsertPdf(sampleEntity(path = "/files/pdfs/a.pdf", name = "a.pdf"))
        dao.upsertPdf(
            sampleEntity(path = "/files/pdfs/a.pdf", name = "a.pdf", favorite = true)
        )

        val all = dao.getAllPdfs()

        assertEquals(1, all.size)
        assertTrue(all.first().isFavorite)
    }

    @Test
    fun getAllPdfs_ordersByLastModifiedDescending() = runBlocking {
        dao.upsertPdfs(
            listOf(
                sampleEntity(path = "/a.pdf", name = "a.pdf", lastModified = 100L),
                sampleEntity(path = "/b.pdf", name = "b.pdf", lastModified = 300L),
                sampleEntity(path = "/c.pdf", name = "c.pdf", lastModified = 200L)
            )
        )

        val ordered = dao.getAllPdfs().map { it.fileName }

        assertEquals(listOf("b.pdf", "c.pdf", "a.pdf"), ordered)
    }

    @Test
    fun deletePdfsNotIn_removesOnlyStaleEntries() = runBlocking {
        dao.upsertPdfs(
            listOf(
                sampleEntity(path = "/keep.pdf", name = "keep.pdf"),
                sampleEntity(path = "/stale.pdf", name = "stale.pdf")
            )
        )

        val deleted = dao.deletePdfsNotIn(listOf("/keep.pdf"))

        assertEquals(1, deleted)
        assertEquals(listOf("keep.pdf"), dao.getAllPdfs().map { it.fileName })
    }

    @Test
    fun deletePdfByPath_removesRowAndReportsCount() = runBlocking {
        dao.upsertPdf(sampleEntity(path = "/a.pdf", name = "a.pdf"))

        assertEquals(1, dao.deletePdfByPath("/a.pdf"))
        assertEquals(0, dao.deletePdfByPath("/missing.pdf"))
        assertNull(dao.getPdfByPath("/a.pdf"))
    }

    @Test
    fun updateFavorite_and_updateFolders_persistChanges() = runBlocking {
        dao.upsertPdf(sampleEntity(path = "/a.pdf", name = "a.pdf"))

        dao.updateFavorite("/a.pdf", true)
        dao.updateFolders("/a.pdf", """["Study","Work"]""")

        val row = dao.getPdfByPath("/a.pdf")
        assertTrue(row!!.isFavorite)
        assertEquals("""["Study","Work"]""", row.foldersJson)
    }

    @Test
    fun categories_upsertAndDelete_workAsExpected() = runBlocking {
        dao.upsertCategories(
            listOf(CategoryEntity(name = "Study", orderIndex = 1), CategoryEntity(name = "Work", orderIndex = 0))
        )

        assertEquals(listOf("Work", "Study"), dao.getCategories().map { it.name })

        dao.deleteCategory("Study")
        assertEquals(listOf("Work"), dao.getCategories().map { it.name })
    }

    @Test
    fun pinnedFolders_preserveOrderIndex() = runBlocking {
        dao.upsertPinnedFolders(
            listOf(
                PinnedFolderEntity(name = "Study", orderIndex = 1),
                PinnedFolderEntity(name = "Favorites", orderIndex = 0)
            )
        )

        assertEquals(
            listOf("Favorites", "Study"),
            dao.getPinnedFolders().map { it.name }
        )
        assertEquals(2, dao.clearPinnedFolders())
        assertEquals(0, dao.getPinnedFolders().size)
    }

    private fun sampleEntity(
        path: String,
        name: String,
        favorite: Boolean = false,
        lastModified: Long = 1_700_000_000_000L
    ) = RecentPdfEntity(
        filePath = path,
        fileName = name,
        fileSizeBytes = 1024L,
        lastModified = lastModified,
        pageCount = 1,
        isFavorite = favorite,
        foldersJson = "[]",
        tagsJson = "[]",
        customTitle = null
    )
}
