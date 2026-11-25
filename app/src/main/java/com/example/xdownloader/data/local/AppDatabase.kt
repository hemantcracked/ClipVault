package com.example.xdownloader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE downloaded_videos ADD COLUMN platform TEXT NOT NULL DEFAULT 'X_TWITTER'")
        db.execSQL("ALTER TABLE downloaded_videos ADD COLUMN mediaType TEXT NOT NULL DEFAULT 'VIDEO'")
    }
}

@Database(
    entities = [DownloadedVideoEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadedVideoDao(): DownloadedVideoDao
}
