package com.example.xdownloader.data.remote.extractor

import com.example.xdownloader.data.remote.api.VxTwitterApi
import com.example.xdownloader.domain.model.ExtractionErrorType
import com.example.xdownloader.domain.model.ExtractionResult
import com.example.xdownloader.domain.model.TweetMetadata
import com.example.xdownloader.domain.model.VideoMediaItem
import com.example.xdownloader.domain.model.VideoVariant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VxTwitterExtractor @Inject constructor(
    private val api: VxTwitterApi
) : TweetVideoExtractor {

    override val name: String = "VxTwitter"

    override suspend fun extract(tweetId: String): ExtractionResult {
        return try {
            val response = api.getTweetStatus(tweetId)
            if (!response.isSuccessful || response.body() == null) {
                return ExtractionResult.Error(
                    message = "VxTwitter returned code ${response.code()}",
                    type = if (response.code() == 404) ExtractionErrorType.PRIVATE_OR_DELETED else ExtractionErrorType.NETWORK_ERROR
                )
            }

            val vx = response.body()!!
            val mediaList = (vx.mediaExtended ?: vx.qTweet?.mediaExtended)?.filter { it.type == "video" || it.type == "gif" }
            if (mediaList.isNullOrEmpty()) {
                return ExtractionResult.Error("No video found in VxTwitter response", ExtractionErrorType.NO_VIDEO)
            }

            val authorHandle = vx.userScreenName.ifBlank { vx.qTweet?.userScreenName ?: "unknown" }
            val authorName = vx.userName.ifBlank { authorHandle }

            val mediaItems = mediaList.mapIndexed { index, media ->
                val durationMs = media.durationMillis
                val width = media.size?.width ?: 0
                val height = media.size?.height ?: 0

                val rawVariants = media.variants?.map { variant ->
                    val resolution = ExtractorUtils.parseResolution(variant.url, width, height)
                    val size = ExtractorUtils.calculateEstimatedSizeBytes(variant.bitrate, durationMs)
                    VideoVariant(
                        url = variant.url,
                        bitrate = variant.bitrate,
                        resolution = resolution,
                        width = width,
                        height = height,
                        mimeType = variant.contentType.ifBlank { "video/mp4" },
                        estimatedSizeBytes = size
                    )
                } ?: emptyList()

                val sortedVariants = ExtractorUtils.filterAndSortMp4Variants(rawVariants)
                VideoMediaItem(
                    id = "${tweetId}_$index",
                    thumbnailUrl = media.thumbnailUrl,
                    variants = sortedVariants,
                    durationMs = durationMs,
                    isGif = media.type == "gif"
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
                    authorAvatar = vx.userProfileImageUrl,
                    text = vx.text,
                    createdAt = vx.date,
                    mediaItems = mediaItems,
                    originalUrl = "https://x.com/$authorHandle/status/$tweetId"
                )
            )
        } catch (e: Exception) {
            ExtractionResult.Error(e.message ?: "Failed to extract via VxTwitter", ExtractionErrorType.NETWORK_ERROR)
        }
    }
}
