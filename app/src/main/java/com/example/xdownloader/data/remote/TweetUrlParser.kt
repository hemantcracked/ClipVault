package com.example.xdownloader.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TweetUrlParser @Inject constructor(
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
        // Direct regex match first
        val directId = extractTweetId(input)
        if (directId != null) {
            return@withContext directId
        }

        // Check if there's a short URL to follow
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
}
