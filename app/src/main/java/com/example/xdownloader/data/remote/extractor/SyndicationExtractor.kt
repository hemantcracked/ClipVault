package com.example.xdownloader.data.remote.extractor

import com.example.xdownloader.data.remote.api.SyndicationApi
import com.example.xdownloader.domain.model.ExtractionErrorType
import com.example.xdownloader.domain.model.ExtractionResult
import com.example.xdownloader.domain.model.TweetMetadata
import com.example.xdownloader.domain.model.VideoMediaItem
import com.example.xdownloader.domain.model.VideoVariant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyndicationExtractor @Inject constructor(
    private val api: SyndicationApi
) : TweetVideoExtractor {

    override val name: String = "Syndication"

    override suspend fun extract(tweetId: String): ExtractionResult {
        return try {
            val response = api.getTweetResult(tweetId)
            if (!response.isSuccessful || response.body() == null) {
                return ExtractionResult.Error(
                    message = "Syndication returned code ${response.code()}",
                    type = if (response.code() == 404) ExtractionErrorType.PRIVATE_OR_DELETED else ExtractionErrorType.NETWORK_ERROR
                )
            }

            val syn = response.body()!!
            val mediaDetails = (syn.mediaDetails ?: syn.quotedTweet?.mediaDetails)?.filter {
                it.type == "video" || it.type == "animated_gif"
            }

            if (mediaDetails.isNullOrEmpty()) {
                return ExtractionResult.Error("No video found in Syndication response", ExtractionErrorType.NO_VIDEO)
            }

            val user = syn.user ?: syn.quotedTweet?.user
            val authorHandle = user?.screenName ?: "unknown"
            val authorName = user?.name ?: authorHandle
            val avatarUrl = user?.profileImageUrlHttps ?: ""

            val mediaItems = mediaDetails.mapIndexed { index, media ->
                val durationMs = media.durationMillis
                val width = media.originalInfo?.width ?: 0
                val height = media.originalInfo?.height ?: 0

                val rawVariants = media.videoInfo?.variants?.map { variant ->
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
                    thumbnailUrl = media.mediaUrlHttps,
                    variants = sortedVariants,
                    durationMs = durationMs,
                    isGif = media.type == "animated_gif"
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
                    authorAvatar = avatarUrl,
                    text = syn.text,
                    createdAt = syn.createdAt,
                    mediaItems = mediaItems,
                    originalUrl = "https://x.com/$authorHandle/status/$tweetId"
                )
            )
        } catch (e: Exception) {
            ExtractionResult.Error(e.message ?: "Failed to extract via Syndication", ExtractionErrorType.NETWORK_ERROR)
        }
    }
}
