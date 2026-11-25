package com.example.xdownloader.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.xdownloader.data.local.DownloadedVideoDao
import com.example.xdownloader.data.local.DownloadedVideoEntity
import com.example.xdownloader.data.remote.ytdlp.YtDlpManager
import com.example.xdownloader.domain.model.DownloadStatus
import com.example.xdownloader.domain.model.MediaPlatform
import com.example.xdownloader.domain.model.MediaType
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

@HiltWorker
class VideoDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val dao: DownloadedVideoDao,
    private val mediaStoreWriter: MediaStoreWriter,
    private val notificationHelper: NotificationHelper,
    private val ytDlpManager: YtDlpManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "YtDlp"
        const val KEY_VIDEO_ID = "video_id"
        const val KEY_TWEET_ID = "tweet_id"
        const val KEY_AUTHOR_HANDLE = "author_handle"
        const val KEY_AUTHOR_NAME = "author_name"
        const val KEY_CAPTION = "caption"
        const val KEY_THUMBNAIL_URL = "thumbnail_url"
        const val KEY_RESOLUTION = "resolution"
        const val KEY_VIDEO_URL = "video_url"
        const val KEY_DURATION_MS = "duration_ms"
        const val KEY_ORIGINAL_URL = "original_url"
        const val KEY_PLATFORM = "platform"
        const val KEY_MEDIA_TYPE = "media_type"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val videoId = inputData.getString(KEY_VIDEO_ID) ?: return@withContext Result.failure()
        val tweetId = inputData.getString(KEY_TWEET_ID) ?: ""
        val authorHandle = inputData.getString(KEY_AUTHOR_HANDLE) ?: "unknown"
        val authorName = inputData.getString(KEY_AUTHOR_NAME) ?: authorHandle
        val caption = inputData.getString(KEY_CAPTION) ?: ""
        val thumbnailUrl = inputData.getString(KEY_THUMBNAIL_URL) ?: ""
        val resolution = inputData.getString(KEY_RESOLUTION) ?: "HD"
        val videoUrl = inputData.getString(KEY_VIDEO_URL) ?: return@withContext Result.failure()
        val durationMs = inputData.getLong(KEY_DURATION_MS, 0L)
        val originalUrl = inputData.getString(KEY_ORIGINAL_URL) ?: ""
        val platformStr = inputData.getString(KEY_PLATFORM) ?: MediaPlatform.X_TWITTER.name
        val isAudioOnly = resolution.contains("Audio", ignoreCase = true) || inputData.getString(KEY_MEDIA_TYPE) == MediaType.IMAGE.name
        val mediaTypeStr = if (isAudioOnly) "AUDIO" else MediaType.VIDEO.name

        val notificationId = kotlin.math.abs(videoId.hashCode())

        // Check duplicate
        val existing = dao.getVideoById(videoId)
        if (existing != null && existing.downloadStatus == DownloadStatus.COMPLETED && mediaStoreWriter.isFileAccessible(existing.mediaStoreUri)) {
            return@withContext Result.success(workDataOf("uri" to existing.mediaStoreUri))
        }

        // Save initial DB record
        val entity = DownloadedVideoEntity(
            id = videoId,
            tweetId = tweetId,
            authorHandle = authorHandle,
            authorName = authorName,
            caption = caption,
            thumbnailUrl = thumbnailUrl,
            resolution = resolution,
            videoUrl = videoUrl,
            fileSizeBytes = 0L,
            durationMs = durationMs,
            mediaStoreUri = "",
            downloadStatus = DownloadStatus.DOWNLOADING,
            createdAt = System.currentTimeMillis(),
            originalTweetUrl = originalUrl,
            platform = platformStr,
            mediaType = mediaTypeStr
        )
        dao.insertVideo(entity)

        val cancelIntent = WorkManager.getInstance(context).createCancelPendingIntent(id)
        try {
            setForeground(notificationHelper.createForegroundInfo(notificationId, "@$authorHandle ($resolution)", 0, cancelIntent))
        } catch (e: Exception) {
            // Ignored
        }

        val sanitizedHandle = authorHandle.replace(Regex("[^a-zA-Z0-9_]"), "")
        val sanitizedRes = resolution.replace(Regex("[^a-zA-Z0-9_]"), "")
        val ext = if (isAudioOnly) "mp3" else "mp4"
        val filename = "ClipVault_${platformStr}_${sanitizedHandle}_${tweetId}_$sanitizedRes.$ext"

        if (platformStr == MediaPlatform.X_TWITTER.name) {
            // Fast direct OkHttp stream path for X/Twitter
            val pendingUri = mediaStoreWriter.createPendingVideoUri(filename, "Movies/ClipVault/X")
                ?: run {
                    dao.updateDownloadStatus(videoId, DownloadStatus.FAILED)
                    return@withContext Result.failure()
                }

            try {
                val request = Request.Builder()
                    .url(videoUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful || response.body == null) {
                    mediaStoreWriter.deleteUri(pendingUri)
                    dao.updateDownloadStatus(videoId, DownloadStatus.FAILED)
                    return@withContext Result.failure()
                }

                val body = response.body!!
                val contentLength = body.contentLength()
                var lastUpdateProgress = -1

                body.byteStream().use { inputStream ->
                    mediaStoreWriter.writeStreamToUri(inputStream, pendingUri, contentLength) { _, progressPercent ->
                        if (isStopped) {
                            throw IOException("Download cancelled by user")
                        }

                        if (progressPercent != lastUpdateProgress) {
                            lastUpdateProgress = progressPercent
                            try {
                                val fgInfo = notificationHelper.createForegroundInfo(
                                    notificationId,
                                    "@$authorHandle ($resolution)",
                                    progressPercent,
                                    cancelIntent
                                )
                                setForegroundAsync(fgInfo)
                            } catch (e: Exception) {
                                // Ignored
                            }
                        }
                    }
                }

                mediaStoreWriter.finishPendingVideoUri(pendingUri)
                val finalUriString = pendingUri.toString()
                val finalSize = if (contentLength > 0) contentLength else 0L
                dao.updateVideoStatus(videoId, DownloadStatus.COMPLETED, finalUriString, finalSize)
                notificationHelper.showCompletionNotification(notificationId, "@$authorHandle ($resolution)", pendingUri)
                Result.success(workDataOf("uri" to finalUriString))
            } catch (e: Exception) {
                mediaStoreWriter.deleteUri(pendingUri)
                dao.updateDownloadStatus(videoId, DownloadStatus.FAILED)
                notificationHelper.showFailureNotification(notificationId, "@$authorHandle", e.message ?: "Failed")
                Result.failure()
            }
        } else {
            // Instagram / YouTube via YtDlp
            val tempDir = File(context.getExternalFilesDir(null), "work_tmp/$videoId").apply { mkdirs() }
            try {
                ytDlpManager.ensureInitialized()

                val request = YoutubeDLRequest(videoUrl).apply {
                    addOption("-o", "${tempDir.absolutePath}/%(id)s.%(ext)s")
                    addOption("--no-playlist")
                    addOption("--no-mtime")
                    addOption("--retries", "5")
                    addOption("--fragment-retries", "5")

                    if (isAudioOnly) {
                        addOption("-x")
                        addOption("--audio-format", "mp3")
                        addOption("--audio-quality", "0")
                    } else {
                        val formatSelector = when {
                            resolution.contains("1080p") -> "bestvideo[height<=1080][ext=mp4]+bestaudio[ext=m4a]/bestvideo[height<=1080]+bestaudio/best[height<=1080]/best"
                            resolution.contains("720p") -> "bestvideo[height<=720][ext=mp4]+bestaudio[ext=m4a]/bestvideo[height<=720]+bestaudio/best[height<=720]/best"
                            resolution.contains("480p") -> "bestvideo[height<=480][ext=mp4]+bestaudio[ext=m4a]/bestvideo[height<=480]+bestaudio/best[height<=480]/best"
                            else -> "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best"
                        }
                        addOption("-f", formatSelector)
                        addOption("--merge-output-format", "mp4")
                    }
                }

                Log.d(TAG, "Executing YtDlp download for $platformStr: $videoUrl")
                var lastProgress = -1

                YoutubeDL.getInstance().execute(request, videoId) { progress, _, _ ->
                    if (isStopped) {
                        try { YoutubeDL.getInstance().destroyProcessById(videoId) } catch (e: Exception) {}
                    }
                    val p = progress.toInt()
                    if (p != lastProgress) {
                        lastProgress = p
                        try {
                            val fgInfo = notificationHelper.createForegroundInfo(notificationId, "@$authorHandle ($resolution)", p, cancelIntent)
                            setForegroundAsync(fgInfo)
                        } catch (e: Exception) {}
                    }
                }

                val downloadedFile = tempDir.listFiles()?.firstOrNull {
                    if (isAudioOnly) it.name.endsWith(".mp3", ignoreCase = true) || it.name.endsWith(".m4a", ignoreCase = true)
                    else it.name.endsWith(".mp4", ignoreCase = true)
                } ?: tempDir.listFiles()?.firstOrNull()
                ?: throw IOException("No output file generated by YoutubeDL")

                val relativePath = when {
                    isAudioOnly -> "Music/ClipVault"
                    platformStr == MediaPlatform.INSTAGRAM.name -> "Movies/ClipVault/Instagram"
                    else -> "Movies/ClipVault/YouTube"
                }

                val pendingUri = if (isAudioOnly) {
                    mediaStoreWriter.createPendingAudioUri(downloadedFile.name, relativePath)
                } else {
                    mediaStoreWriter.createPendingVideoUri(downloadedFile.name, relativePath)
                } ?: throw IOException("Failed to create MediaStore entry")

                downloadedFile.inputStream().use { input ->
                    mediaStoreWriter.writeStreamToUri(input, pendingUri, downloadedFile.length())
                }

                mediaStoreWriter.finishPendingVideoUri(pendingUri)
                val finalUriString = pendingUri.toString()
                val finalSize = downloadedFile.length()

                dao.updateVideoStatus(videoId, DownloadStatus.COMPLETED, finalUriString, finalSize)
                notificationHelper.showCompletionNotification(notificationId, "@$authorHandle ($resolution)", pendingUri)
                Result.success(workDataOf("uri" to finalUriString))
            } catch (e: Exception) {
                Log.e(TAG, "YtDlp download failed: ${e.message}", e)
                dao.updateDownloadStatus(videoId, DownloadStatus.FAILED)
                notificationHelper.showFailureNotification(notificationId, "@$authorHandle", e.message ?: "Failed")
                Result.failure()
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }
}
