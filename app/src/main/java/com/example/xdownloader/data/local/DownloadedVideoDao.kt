package com.example.xdownloader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.xdownloader.domain.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedVideoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: DownloadedVideoEntity)

    @Query("UPDATE downloaded_videos SET downloadStatus = :status, mediaStoreUri = :mediaStoreUri, fileSizeBytes = :fileSizeBytes WHERE id = :id")
    suspend fun updateVideoStatus(id: String, status: DownloadStatus, mediaStoreUri: String, fileSizeBytes: Long)

    @Query("UPDATE downloaded_videos SET downloadStatus = :status WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, status: DownloadStatus)

    @Query("DELETE FROM downloaded_videos WHERE id = :id")
    suspend fun deleteVideoById(id: String)

    @Query("SELECT * FROM downloaded_videos ORDER BY createdAt DESC")
    fun getAllVideosFlow(): Flow<List<DownloadedVideoEntity>>

    @Query("SELECT * FROM downloaded_videos WHERE downloadStatus = 'COMPLETED' ORDER BY createdAt DESC")
    fun getCompletedVideosFlow(): Flow<List<DownloadedVideoEntity>>

    @Query("SELECT * FROM downloaded_videos WHERE downloadStatus = 'COMPLETED' AND (:platform = 'ALL' OR platform = :platform) ORDER BY createdAt DESC")
    fun getCompletedVideosByPlatformFlow(platform: String): Flow<List<DownloadedVideoEntity>>

    @Query("SELECT * FROM downloaded_videos WHERE authorHandle LIKE '%' || :query || '%' OR authorName LIKE '%' || :query || '%' OR caption LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchVideosFlow(query: String): Flow<List<DownloadedVideoEntity>>

    @Query("SELECT * FROM downloaded_videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: String): DownloadedVideoEntity?

    @Query("SELECT * FROM downloaded_videos WHERE tweetId = :tweetId AND resolution = :resolution AND downloadStatus = 'COMPLETED' LIMIT 1")
    suspend fun findCompletedDownload(tweetId: String, resolution: String): DownloadedVideoEntity?
}
