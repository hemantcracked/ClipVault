package com.example.xdownloader

import com.example.xdownloader.data.remote.MediaUrlRouter
import com.example.xdownloader.domain.model.MediaPlatform
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TweetUrlParserTest {

    private lateinit var router: MediaUrlRouter

    @Before
    fun setUp() {
        router = MediaUrlRouter(okHttpClient = mockk(relaxed = true))
    }

    @Test
    fun detectPlatform_xAndTwitterAndShortUrls_returnsXPlatform() {
        assertEquals(MediaPlatform.X_TWITTER, router.detectPlatform("https://x.com/user/status/123"))
        assertEquals(MediaPlatform.X_TWITTER, router.detectPlatform("https://twitter.com/user/status/123"))
        assertEquals(MediaPlatform.X_TWITTER, router.detectPlatform("https://t.co/xyz123"))
    }

    @Test
    fun detectPlatform_instagramUrls_returnsInstagramPlatform() {
        assertEquals(MediaPlatform.INSTAGRAM, router.detectPlatform("https://www.instagram.com/reel/C12345/"))
        assertEquals(MediaPlatform.INSTAGRAM, router.detectPlatform("https://instagr.am/p/C67890/"))
    }

    @Test
    fun detectPlatform_youtubeUrls_returnsYouTubePlatform() {
        assertEquals(MediaPlatform.YOUTUBE, router.detectPlatform("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals(MediaPlatform.YOUTUBE, router.detectPlatform("https://youtu.be/dQw4w9WgXcQ"))
    }

    @Test
    fun extractTweetId_standardXUrl_returnsId() {
        val url = "https://x.com/AndroidDev/status/1890000000000000000"
        val tweetId = router.extractTweetId(url)
        assertEquals("1890000000000000000", tweetId)
    }

    @Test
    fun extractTweetId_twitterUrlWithQueryParams_returnsId() {
        val url = "https://twitter.com/user/status/123456789?s=20&t=abcdef"
        val tweetId = router.extractTweetId(url)
        assertEquals("123456789", tweetId)
    }

    @Test
    fun extractTweetId_mobileXUrl_returnsId() {
        val url = "https://mobile.x.com/user/status/987654321"
        val tweetId = router.extractTweetId(url)
        assertEquals("987654321", tweetId)
    }

    @Test
    fun extractTweetId_iStatusFormat_returnsId() {
        val url = "https://x.com/i/status/555444333"
        val tweetId = router.extractTweetId(url)
        assertEquals("555444333", tweetId)
    }

    @Test
    fun extractTweetId_invalidUrl_returnsNull() {
        val url = "https://example.com/not_a_tweet"
        val tweetId = router.extractTweetId(url)
        assertNull(tweetId)
    }

    @Test
    fun extractShortUrl_tCoLinkInText_returnsShortUrl() {
        val text = "Check out this video: https://t.co/XyZ123abc4 for details!"
        val shortUrl = router.extractShortUrl(text)
        assertEquals("https://t.co/XyZ123abc4", shortUrl)
    }
}
