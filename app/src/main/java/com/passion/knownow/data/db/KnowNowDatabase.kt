package com.passion.knownow.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ReleaseEntity::class,
        PublicationEntity::class,
        DocumentEntity::class,
        BookmarkEntity::class,
        NoteEntity::class,
        ReadingProgressEntity::class,
        HighlightEntity::class,
        DownloadTaskEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class KnowNowDatabase : RoomDatabase() {
    abstract fun releaseDao(): ReleaseDao
    abstract fun publicationDao(): PublicationDao
    abstract fun documentDao(): DocumentDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun noteDao(): NoteDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun highlightDao(): HighlightDao
    abstract fun downloadTaskDao(): DownloadTaskDao
    abstract fun libraryDao(): LibraryDao

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `download_tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `publicationId` INTEGER NOT NULL,
                        `releaseName` TEXT NOT NULL,
                        `slug` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `state` TEXT NOT NULL,
                        `total` INTEGER NOT NULL,
                        `completed` INTEGER NOT NULL,
                        `bytes` INTEGER NOT NULL,
                        `error` TEXT,
                        `attempts` INTEGER NOT NULL,
                        `forceRefresh` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_download_tasks_publicationId` ON `download_tasks` (`publicationId`)")
            }
        }
    }
}
