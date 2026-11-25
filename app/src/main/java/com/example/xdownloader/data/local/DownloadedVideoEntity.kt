package com.example.xdownloader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.xdownloader.domain.model.DownloadStatus

@Entity(tableName = "downloaded_videos")
data class DownloadedVideoEntity(
    @PrimaryKey
    val id: String,
    val tweetId: String,
    val authorHandle: String,
    val authorName: String,
    val caption: String,
    val thumbnailUrl: String,
    val resolution: String,
    val videoUrl: String,
    val fileSizeBytes: Long = 0L,
    val durationMs: Long = 0L,
    val mediaStoreUri: String = "",
    val downloadStatus: DownloadStatus = DownloadStatus.QUEUED,
    val createdAt: Long = System.currentTimeMillis(),
    val originalTweetUrl: String = "",
    val platform: String = "X_TWITTER",
    val mediaType: String = "VIDEO"
)
