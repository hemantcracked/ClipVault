package com.example.xdownloader.data.remote.extractor

import com.example.xdownloader.data.remote.api.FxTwitterApi
import com.example.xdownloader.domain.model.ExtractionErrorType
import com.example.xdownloader.domain.model.ExtractionResult
import com.example.xdownloader.domain.model.TweetMetadata
import com.example.xdownloader.domain.model.VideoMediaItem
import com.example.xdownloader.domain.model.VideoVariant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FxTwitterExtractor @Inject constructor(
    private val api: FxTwitterApi
) : TweetVideoExtractor {

    override val name: String = "FxTwitter"

    override suspend fun extract(tweetId: String): ExtractionResult {
        return try {
            val response = api.getTweetStatus(tweetId)
            if (!response.isSuccessful || response.body() == null) {
                return ExtractionResult.Error(
                    message = "FxTwitter returned code ${response.code()}",
                    type = if (response.code() == 404) ExtractionErrorType.PRIVATE_OR_DELETED else ExtractionErrorType.NETWORK_ERROR
                )
            }

            val fxResponse = response.body()!!
            val tweet = fxResponse.tweet ?: return ExtractionResult.Error("Tweet not found in FxTwitter response", ExtractionErrorType.PRIVATE_OR_DELETED)

            val videos = tweet.media?.videos ?: tweet.quote?.media?.videos
            if (videos.isNullOrEmpty()) {
                return ExtractionResult.Error("No video found in this tweet", ExtractionErrorType.NO_VIDEO)
            }

            val author = tweet.author ?: tweet.quote?.author
            val authorHandle = author?.screenName ?: "unknown"
            val authorName = author?.name ?: authorHandle
            val authorAvatar = author?.avatarUrl ?: ""
            val caption = tweet.text.ifBlank { tweet.quote?.text ?: "" }
            val createdAt = tweet.createdAt

            val mediaItems = videos.mapIndexed { index, video ->
                val durationMs = (video.duration * 1000).toLong()
                val rawVariants = video.variants?.map { variant ->
                    val resolution = ExtractorUtils.parseResolution(variant.url, video.width, video.height)
                    val size = ExtractorUtils.calculateEstimatedSizeBytes(variant.bitrate, durationMs)
                    VideoVariant(
                        url = variant.url,
                        bitrate = variant.bitrate,
                        resolution = resolution,
                        width = video.width,
                        height = video.height,
                        mimeType = variant.contentType.ifBlank { "video/mp4" },
                        estimatedSizeBytes = size
                    )
                } ?: emptyList()

                val sortedVariants = ExtractorUtils.filterAndSortMp4Variants(rawVariants)
                val isGif = video.type.equals("gif", ignoreCase = true) || video.format.contains("gif", ignoreCase = true)

                VideoMediaItem(
                    id = "${tweetId}_$index",
                    thumbnailUrl = video.thumbnailUrl,
                    variants = sortedVariants,
                    durationMs = durationMs,
                    isGif = isGif
                )
            }.filter { it.variants.isNotEmpty() }

            if (mediaItems.isEmpty()) {
                return ExtractionResult.Error("No downloadable MP4 video formats found", ExtractionErrorType.UNSUPPORTED_TYPE)
            }

            ExtractionResult.Success(
                TweetMetadata(
                    tweetId = tweetId,
                    authorHandle = authorHandle,
                    authorName = authorName,
                    authorAvatar = authorAvatar,
                    text = caption,
                    createdAt = createdAt,
                    mediaItems = mediaItems,
                    originalUrl = "https://x.com/$authorHandle/status/$tweetId"
                )
            )
        } catch (e: Exception) {
            ExtractionResult.Error(e.message ?: "Failed to extract via FxTwitter", ExtractionErrorType.NETWORK_ERROR)
        }
    }
}
