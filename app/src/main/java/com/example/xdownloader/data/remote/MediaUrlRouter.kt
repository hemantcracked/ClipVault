package com.example.xdownloader.data.remote

import com.example.xdownloader.domain.model.MediaPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaUrlRouter @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val tweetPattern: Pattern = Pattern.compile(
        "https?://(?:www\\.|mobile\\.)?(?:twitter\\.com|x\\.com)/(?:#!/)?(?:\\w+/status/|i/status/|status/)(\\d+)",
        Pattern.CASE_INSENSITIVE
    )

    private val shortUrlPattern: Pattern = Pattern.compile(
        "https?://t\\.co/[a-zA-Z0-9]+",
        Pattern.CASE_INSENSITIVE
    )

    private val instagramPattern: Pattern = Pattern.compile(
        "https?://(?:www\\.)?(?:instagram\\.com|instagr\\.am)/(?:p|reel|reels|tv|share)/(?:[a-zA-Z0-9_-]+)",
        Pattern.CASE_INSENSITIVE
    )

    private val youtubePattern: Pattern = Pattern.compile(
        "https?://(?:www\\.|m\\.|music\\.)?(?:youtube\\.com/(?:watch\\?[^\\s]+|shorts/[A-Za-z0-9_-]+[^\\s]*|live/[A-Za-z0-9_-]+[^\\s]*)|youtu\\.be/[A-Za-z0-9_-]+[^\\s]*)",
        Pattern.CASE_INSENSITIVE
    )

    fun detectPlatform(input: String): MediaPlatform? {
        val lower = input.lowercase()
        return when {
            lower.contains("x.com") || lower.contains("twitter.com") || lower.contains("t.co/") -> MediaPlatform.X_TWITTER
            lower.contains("instagram.com") || lower.contains("instagr.am") -> MediaPlatform.INSTAGRAM
            lower.contains("youtube.com") || lower.contains("youtu.be") -> MediaPlatform.YOUTUBE
            else -> null
        }
    }

    fun extractYouTubeUrl(input: String): String? {
        val matcher = youtubePattern.matcher(input)
        if (matcher.find()) {
            return matcher.group(0)
        }
        return null
    }

    fun extractTweetId(input: String): String? {
        val matcher = tweetPattern.matcher(input)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    fun extractShortUrl(input: String): String? {
        val matcher = shortUrlPattern.matcher(input)
        if (matcher.find()) {
            return matcher.group(0)
        }
        return null
    }

    suspend fun resolveAndExtractTweetId(input: String): String? = withContext(Dispatchers.IO) {
        val directId = extractTweetId(input)
        if (directId != null) return@withContext directId

        val shortUrl = extractShortUrl(input) ?: return@withContext null
        try {
            val request = Request.Builder()
                .url(shortUrl)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val finalUrl = response.request.url.toString()
            response.close()

            extractTweetId(finalUrl)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun resolveInstagramUrl(input: String): String = withContext(Dispatchers.IO) {
        val matcher = instagramPattern.matcher(input)
        val extractedUrl = if (matcher.find()) matcher.group() else input

        if (extractedUrl.contains("/share/")) {
            try {
                val request = Request.Builder()
                    .url(extractedUrl)
                    .head()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val finalUrl = response.request.url.toString()
                response.close()
                return@withContext finalUrl
            } catch (e: Exception) {
                // Fallback
            }
        }
        extractedUrl
    }
}
