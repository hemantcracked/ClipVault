package com.example.xdownloader.data.remote.extractor

import android.util.Log
import com.example.xdownloader.data.remote.MediaUrlRouter
import com.example.xdownloader.data.remote.ytdlp.YtDlpManager
import com.example.xdownloader.domain.model.ExtractionErrorType
import com.example.xdownloader.domain.model.ExtractionResult
import com.example.xdownloader.domain.model.TweetMetadata
import com.example.xdownloader.domain.model.VideoMediaItem
import com.example.xdownloader.domain.model.VideoVariant
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeExtractor @Inject constructor(
    private val mediaUrlRouter: MediaUrlRouter,
    private val ytDlpManager: YtDlpManager
) {
    companion object {
        private const val TAG = "YtDlp"
    }

    suspend fun extract(inputUrl: String): ExtractionResult = withContext(Dispatchers.IO) {
        val targetUrl = mediaUrlRouter.extractYouTubeUrl(inputUrl) ?: inputUrl
        Log.d(TAG, "YT_GET_INFO_START url=$targetUrl")

        if (!ytDlpManager.ensureInitialized()) {
            Log.e(TAG, "YT_ERROR: YtDlp engine initialization failed")
            return@withContext ExtractionResult.Error(
                "YouTube extractor engine initialization failed",
                ExtractionErrorType.NETWORK_ERROR
            )
        }

        try {
            val request = YoutubeDLRequest(targetUrl).apply {
                addOption("--no-playlist")
            }

            val videoInfo = withTimeout(25_000L) {
                YoutubeDL.getInstance().getInfo(request)
            }

            val title = videoInfo.title ?: videoInfo.description ?: "YouTube Video"
            val uploader = videoInfo.uploader ?: videoInfo.uploaderId ?: "YouTube Channel"
            val thumbnailUrl = videoInfo.thumbnail ?: ""
            val durationMs = (videoInfo.duration * 1000).toLong()
            val videoId = videoInfo.id ?: "yt_${System.currentTimeMillis()}"

            Log.d(TAG, "YT_GET_INFO_OK title=$title uploader=$uploader")

            val variants = listOf(
                VideoVariant(
                    url = targetUrl,
                    bitrate = 2500000,
                    resolution = "1080p HD (MP4)",
                    width = 1080,
                    height = 1920,
                    mimeType = "video/mp4"
                ),
                VideoVariant(
                    url = targetUrl,
                    bitrate = 1200000,
                    resolution = "720p HD (MP4)",
                    width = 720,
                    height = 1280,
                    mimeType = "video/mp4"
                ),
                VideoVariant(
                    url = targetUrl,
                    bitrate = 600000,
                    resolution = "480p Saver (MP4)",
                    width = 480,
                    height = 854,
                    mimeType = "video/mp4"
                ),
                VideoVariant(
                    url = targetUrl,
                    bitrate = 320000,
                    resolution = "Audio Only (MP3)",
                    width = 0,
                    height = 0,
                    mimeType = "audio/mp3"
                )
            )

            val mediaItem = VideoMediaItem(
                id = videoId,
                thumbnailUrl = thumbnailUrl,
                variants = variants,
                durationMs = durationMs
            )

            ExtractionResult.Success(
                TweetMetadata(
                    tweetId = videoId,
                    authorHandle = uploader,
                    authorName = uploader,
                    authorAvatar = thumbnailUrl,
                    text = title,
                    createdAt = "",
                    mediaItems = listOf(mediaItem),
                    originalUrl = targetUrl
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "YT_ERROR: ${e.message}", e)
            ExtractionResult.Error(
                message = e.message ?: "Failed to extract YouTube video metadata",
                type = ExtractionErrorType.NETWORK_ERROR
            )
        }
    }
}
