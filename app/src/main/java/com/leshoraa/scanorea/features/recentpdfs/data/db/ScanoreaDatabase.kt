package com.leshoraa.scanorea.features.recentpdfs.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Room database instance for Scanorea, storing document metadata,
 * folders, and pinned state with zero-overhead indexing.
 */
@Database(
    entities = [
        RecentPdfEntity::class,
        CategoryEntity::class,
        PinnedFolderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ScanoreaDatabase : RoomDatabase() {

    abstract fun recentPdfsDao(): RecentPdfsDao

    companion object {
        private const val DATABASE_NAME = "scanorea_database.db"

        @Volatile
        private var INSTANCE: ScanoreaDatabase? = null

        fun getInstance(context: Context): ScanoreaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): ScanoreaDatabase {
            val appContext = context.applicationContext ?: context
            return Room.databaseBuilder(
                appContext,
                ScanoreaDatabase::class.java,
                DATABASE_NAME
            ).fallbackToDestructiveMigration(dropAllTables = true).build()
        }
    }
}
