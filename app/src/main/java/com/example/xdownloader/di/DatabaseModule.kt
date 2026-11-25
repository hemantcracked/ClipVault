package com.example.xdownloader.di

import android.content.Context
import androidx.room.Room
import com.example.xdownloader.data.local.AppDatabase
import com.example.xdownloader.data.local.DownloadedVideoDao
import com.example.xdownloader.data.local.MIGRATION_1_2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "xdownloader.db"
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideDownloadedVideoDao(database: AppDatabase): DownloadedVideoDao {
        return database.downloadedVideoDao()
    }
}
