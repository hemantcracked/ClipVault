package com.example.xdownloader.data.remote.extractor

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
class InstagramExtractor @Inject constructor(
    private val mediaUrlRouter: MediaUrlRouter,
    private val ytDlpManager: YtDlpManager
) {
    val name: String = "Instagram"

    suspend fun extract(inputUrl: String): ExtractionResult = withContext(Dispatchers.IO) {
        val resolvedUrl = mediaUrlRouter.resolveInstagramUrl(inputUrl)
        if (!ytDlpManager.ensureInitialized()) {
            return@withContext ExtractionResult.Error(
                "YtDlp engine initialization failed",
                ExtractionErrorType.NETWORK_ERROR
            )
        }

        try {
            val request = YoutubeDLRequest(resolvedUrl).apply {
                addOption("--no-playlist")
            }

            val videoInfo = withTimeout(20_000L) {
                YoutubeDL.getInstance().getInfo(request)
            }

            val title = videoInfo.title ?: videoInfo.description ?: "Instagram Post"
            val uploader = videoInfo.uploader ?: videoInfo.uploaderId ?: "instagram_user"
            val thumbnailUrl = videoInfo.thumbnail ?: ""
            val durationMs = (videoInfo.duration * 1000).toLong()
            val mediaId = videoInfo.id ?: "ig_${System.currentTimeMillis()}"

            // Generate clean variant choices for UI
            val variants = listOf(
                VideoVariant(
                    url = resolvedUrl,
                    bitrate = 2500000,
                    resolution = "1080p (HD)",
                    width = 1080,
                    height = 1920,
                    mimeType = "video/mp4",
                    estimatedSizeBytes = 0L
                ),
                VideoVariant(
                    url = resolvedUrl,
                    bitrate = 1200000,
                    resolution = "720p (HD)",
                    width = 720,
                    height = 1280,
                    mimeType = "video/mp4",
                    estimatedSizeBytes = 0L
                )
            )

            val mediaItem = VideoMediaItem(
                id = mediaId,
                thumbnailUrl = thumbnailUrl,
                variants = variants,
                durationMs = durationMs
            )

            ExtractionResult.Success(
                TweetMetadata(
                    tweetId = mediaId,
                    authorHandle = uploader,
                    authorName = uploader,
                    authorAvatar = thumbnailUrl,
                    text = title,
                    createdAt = "",
                    mediaItems = listOf(mediaItem),
                    originalUrl = resolvedUrl
                )
            )
        } catch (e: Exception) {
            ExtractionResult.Error(
                message = e.message ?: "Failed to extract Instagram media",
                type = ExtractionErrorType.NETWORK_ERROR
            )
        }
    }
}
