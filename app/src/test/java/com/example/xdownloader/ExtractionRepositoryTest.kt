package com.example.xdownloader

import com.example.xdownloader.data.remote.MediaUrlRouter
import com.example.xdownloader.data.remote.extractor.FxTwitterExtractor
import com.example.xdownloader.data.remote.extractor.InstagramExtractor
import com.example.xdownloader.data.remote.extractor.SyndicationExtractor
import com.example.xdownloader.data.remote.extractor.VxTwitterExtractor
import com.example.xdownloader.data.remote.extractor.YouTubeExtractor
import com.example.xdownloader.data.repository.ExtractionRepository
import com.example.xdownloader.domain.model.ExtractionErrorType
import com.example.xdownloader.domain.model.ExtractionResult
import com.example.xdownloader.domain.model.MediaPlatform
import com.example.xdownloader.domain.model.TweetMetadata
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExtractionRepositoryTest {

    private val mediaUrlRouter: MediaUrlRouter = mockk()
    private val instagramExtractor: InstagramExtractor = mockk()
    private val youTubeExtractor: YouTubeExtractor = mockk()
    private val fxTwitterExtractor: FxTwitterExtractor = mockk()
    private val vxTwitterExtractor: VxTwitterExtractor = mockk()
    private val syndicationExtractor: SyndicationExtractor = mockk()

    private lateinit var repository: ExtractionRepository

    @Before
    fun setUp() {
        repository = ExtractionRepository(
            mediaUrlRouter = mediaUrlRouter,
            instagramExtractor = instagramExtractor,
            youTubeExtractor = youTubeExtractor,
            fxTwitterExtractor = fxTwitterExtractor,
            vxTwitterExtractor = vxTwitterExtractor,
            syndicationExtractor = syndicationExtractor
        )
    }

    @Test
    fun extractVideo_primarySucceeds_returnsPrimaryResult() = runTest {
        val input = "https://x.com/user/status/100"
        val metadata = TweetMetadata("100", "user", "User Name", "", "Test caption", "", emptyList(), input)

        coEvery { mediaUrlRouter.detectPlatform(input) } returns MediaPlatform.X_TWITTER
        coEvery { mediaUrlRouter.resolveAndExtractTweetId(input) } returns "100"
        coEvery { fxTwitterExtractor.extract("100") } returns ExtractionResult.Success(metadata)

        val result = repository.extractVideo(input)

        assertTrue(result is ExtractionResult.Success)
        assertEquals("100", (result as ExtractionResult.Success).metadata.tweetId)
        coVerify(exactly = 0) { vxTwitterExtractor.extract(any()) }
    }

    @Test
    fun extractVideo_primaryFailsNetworkError_fallbackSucceeds() = runTest {
        val input = "https://x.com/user/status/200"
        val metadata = TweetMetadata("200", "user", "User Name", "", "Fallback success", "", emptyList(), input)

        coEvery { mediaUrlRouter.detectPlatform(input) } returns MediaPlatform.X_TWITTER
        coEvery { mediaUrlRouter.resolveAndExtractTweetId(input) } returns "200"
        coEvery { fxTwitterExtractor.extract("200") } returns ExtractionResult.Error("Network error", ExtractionErrorType.NETWORK_ERROR)
        coEvery { vxTwitterExtractor.extract("200") } returns ExtractionResult.Success(metadata)

        val result = repository.extractVideo(input)

        assertTrue(result is ExtractionResult.Success)
        assertEquals("200", (result as ExtractionResult.Success).metadata.tweetId)
        coVerify(exactly = 1) { fxTwitterExtractor.extract("200") }
        coVerify(exactly = 1) { vxTwitterExtractor.extract("200") }
    }

    @Test
    fun extractVideo_primaryReturnsPrivateOrDeleted_shortCircuitsWithoutFallback() = runTest {
        val input = "https://x.com/user/status/300"

        coEvery { mediaUrlRouter.detectPlatform(input) } returns MediaPlatform.X_TWITTER
        coEvery { mediaUrlRouter.resolveAndExtractTweetId(input) } returns "300"
        coEvery { fxTwitterExtractor.extract("300") } returns ExtractionResult.Error("Tweet deleted", ExtractionErrorType.PRIVATE_OR_DELETED)

        val result = repository.extractVideo(input)

        assertTrue(result is ExtractionResult.Error)
        assertEquals(ExtractionErrorType.PRIVATE_OR_DELETED, (result as ExtractionResult.Error).type)
        coVerify(exactly = 0) { vxTwitterExtractor.extract(any()) }
        coVerify(exactly = 0) { syndicationExtractor.extract(any()) }
    }

    @Test
    fun extractVideo_instagramPlatform_delegatesToInstagramExtractor() = runTest {
        val input = "https://instagram.com/reel/C12345/"
        val metadata = TweetMetadata("ig_100", "user", "User Name", "", "Instagram Reel", "", emptyList(), input)

        coEvery { mediaUrlRouter.detectPlatform(input) } returns MediaPlatform.INSTAGRAM
        coEvery { instagramExtractor.extract(input) } returns ExtractionResult.Success(metadata)

        val result = repository.extractVideo(input)

        assertTrue(result is ExtractionResult.Success)
        assertEquals("ig_100", (result as ExtractionResult.Success).metadata.tweetId)
        coVerify(exactly = 1) { instagramExtractor.extract(input) }
        coVerify(exactly = 0) { fxTwitterExtractor.extract(any()) }
    }

    @Test
    fun extractVideo_youtubePlatform_delegatesToYouTubeExtractor() = runTest {
        val input = "https://youtu.be/dQw4w9WgXcQ"
        val metadata = TweetMetadata("yt_100", "user", "User Name", "", "YouTube Video", "", emptyList(), input)

        coEvery { mediaUrlRouter.detectPlatform(input) } returns MediaPlatform.YOUTUBE
        coEvery { youTubeExtractor.extract(input) } returns ExtractionResult.Success(metadata)

        val result = repository.extractVideo(input)

        assertTrue(result is ExtractionResult.Success)
        assertEquals("yt_100", (result as ExtractionResult.Success).metadata.tweetId)
        coVerify(exactly = 1) { youTubeExtractor.extract(input) }
        coVerify(exactly = 0) { fxTwitterExtractor.extract(any()) }
    }
}
