package com.example.xdownloader.data.repository

import com.example.xdownloader.data.remote.MediaUrlRouter
import com.example.xdownloader.data.remote.extractor.FxTwitterExtractor
import com.example.xdownloader.data.remote.extractor.InstagramExtractor
import com.example.xdownloader.data.remote.extractor.SyndicationExtractor
import com.example.xdownloader.data.remote.extractor.TweetVideoExtractor
import com.example.xdownloader.data.remote.extractor.VxTwitterExtractor
import com.example.xdownloader.data.remote.extractor.YouTubeExtractor
import com.example.xdownloader.domain.model.ExtractionErrorType
import com.example.xdownloader.domain.model.ExtractionResult
import com.example.xdownloader.domain.model.MediaPlatform
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractionRepository @Inject constructor(
    private val mediaUrlRouter: MediaUrlRouter,
    private val instagramExtractor: InstagramExtractor,
    private val youTubeExtractor: YouTubeExtractor,
    fxTwitterExtractor: FxTwitterExtractor,
    vxTwitterExtractor: VxTwitterExtractor,
    syndicationExtractor: SyndicationExtractor
) {
    private val twitterExtractors: List<TweetVideoExtractor> = listOf(
        fxTwitterExtractor,
        vxTwitterExtractor,
        syndicationExtractor
    )

    fun detectPlatform(input: String): MediaPlatform? {
        return mediaUrlRouter.detectPlatform(input)
    }

    suspend fun extractVideo(input: String): ExtractionResult {
        val platform = mediaUrlRouter.detectPlatform(input)
        if (platform == MediaPlatform.INSTAGRAM) {
            return instagramExtractor.extract(input)
        }
        if (platform == MediaPlatform.YOUTUBE) {
            return youTubeExtractor.extract(input)
        }

        // Default to X/Twitter extraction chain
        val tweetId = mediaUrlRouter.resolveAndExtractTweetId(input)
            ?: return ExtractionResult.Error(
                message = "Could not parse a valid media link.",
                type = ExtractionErrorType.NO_VIDEO
            )

        var lastError: ExtractionResult.Error? = null

        for (extractor in twitterExtractors) {
            when (val result = extractor.extract(tweetId)) {
                is ExtractionResult.Success -> return result
                is ExtractionResult.Error -> {
                    lastError = result
                    if (result.type == ExtractionErrorType.NO_VIDEO || result.type == ExtractionErrorType.PRIVATE_OR_DELETED) {
                        return result
                    }
                }
            }
        }

        return lastError ?: ExtractionResult.Error(
            message = "Failed to extract video metadata from all sources.",
            type = ExtractionErrorType.NETWORK_ERROR
        )
    }
}
