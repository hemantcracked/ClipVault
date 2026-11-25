package com.example.xdownloader.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.xdownloader.domain.model.MediaPlatform
import com.example.xdownloader.domain.model.MediaType
import com.example.xdownloader.domain.model.TweetMetadata
import com.example.xdownloader.domain.model.VideoVariant
import com.example.xdownloader.worker.VideoDownloadWorker

object WorkManagerUtils {

    fun enqueueVideoDownload(
        context: Context,
        metadata: TweetMetadata,
        variant: VideoVariant,
        wifiOnly: Boolean = false,
        platform: MediaPlatform = MediaPlatform.X_TWITTER,
        mediaType: MediaType = MediaType.VIDEO
    ): String {
        val videoId = "${metadata.tweetId}_${variant.resolution.replace(" ", "_")}"

        val inputData = Data.Builder()
            .putString(VideoDownloadWorker.KEY_VIDEO_ID, videoId)
            .putString(VideoDownloadWorker.KEY_TWEET_ID, metadata.tweetId)
            .putString(VideoDownloadWorker.KEY_AUTHOR_HANDLE, metadata.authorHandle)
            .putString(VideoDownloadWorker.KEY_AUTHOR_NAME, metadata.authorName)
            .putString(VideoDownloadWorker.KEY_CAPTION, metadata.text)
            .putString(VideoDownloadWorker.KEY_THUMBNAIL_URL, metadata.mediaItems.firstOrNull()?.thumbnailUrl ?: "")
            .putString(VideoDownloadWorker.KEY_RESOLUTION, variant.resolution)
            .putString(VideoDownloadWorker.KEY_VIDEO_URL, variant.url)
            .putLong(VideoDownloadWorker.KEY_DURATION_MS, metadata.mediaItems.firstOrNull()?.durationMs ?: 0L)
            .putString(VideoDownloadWorker.KEY_ORIGINAL_URL, metadata.originalUrl)
            .putString(VideoDownloadWorker.KEY_PLATFORM, platform.name)
            .putString(VideoDownloadWorker.KEY_MEDIA_TYPE, mediaType.name)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            videoId,
            ExistingWorkPolicy.KEEP,
            downloadRequest
        )

        return videoId
    }
}
