package com.passion.knownow.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ReleaseEntity::class,
        PublicationEntity::class,
        DocumentEntity::class,
        BookmarkEntity::class,
        NoteEntity::class,
        ReadingProgressEntity::class,
        HighlightEntity::class,
    ],
    version = 6,
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
}