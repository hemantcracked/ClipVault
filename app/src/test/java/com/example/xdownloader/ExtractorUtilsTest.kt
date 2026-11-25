package com.example.xdownloader

import com.example.xdownloader.data.remote.extractor.ExtractorUtils
import com.example.xdownloader.domain.model.VideoVariant
import org.junit.Assert.assertEquals
import org.junit.Test

class ExtractorUtilsTest {

    @Test
    fun parseResolution_1080pDimensions_returns1080pHD() {
        val res = ExtractorUtils.parseResolution("https://video.twimg.com/vid/1080x1920/test.mp4", 1080, 1920)
        assertEquals("1080p (HD)", res)
    }

    @Test
    fun parseResolution_720pUrl_returns720pHD() {
        val res = ExtractorUtils.parseResolution("https://video.twimg.com/vid/720x1280/test.mp4", 0, 0)
        assertEquals("720p (HD)", res)
    }

    @Test
    fun parseResolution_480pUrl_returns480pSD() {
        val res = ExtractorUtils.parseResolution("https://video.twimg.com/vid/480x852/test.mp4", 480, 852)
        assertEquals("480p (SD)", res)
    }

    @Test
    fun calculateEstimatedSizeBytes_validBitrateAndDuration_returnsCorrectSize() {
        // 2,000,000 bps for 10 seconds (10,000 ms) -> (2,000,000 * 10,000) / 8000 = 2,500,000 bytes (~2.5 MB)
        val size = ExtractorUtils.calculateEstimatedSizeBytes(2000000, 10000L)
        assertEquals(2500000L, size)
    }

    @Test
    fun filterAndSortMp4Variants_sortsDescendingByBitrate() {
        val variants = listOf(
            VideoVariant(url = "https://cdn/360.mp4", bitrate = 500000, resolution = "360p", width = 360, height = 640, mimeType = "video/mp4"),
            VideoVariant(url = "https://cdn/1080.mp4", bitrate = 2500000, resolution = "1080p", width = 1080, height = 1920, mimeType = "video/mp4"),
            VideoVariant(url = "https://cdn/720.mp4", bitrate = 1200000, resolution = "720p", width = 720, height = 1280, mimeType = "video/mp4"),
            VideoVariant(url = "https://cdn/stream.m3u8", bitrate = 0, resolution = "HLS", width = 0, height = 0, mimeType = "application/x-mpegURL")
        )

        val sorted = ExtractorUtils.filterAndSortMp4Variants(variants)
        assertEquals(3, sorted.size)
        assertEquals("https://cdn/1080.mp4", sorted[0].url)
        assertEquals("https://cdn/720.mp4", sorted[1].url)
        assertEquals("https://cdn/360.mp4", sorted[2].url)
    }
}
