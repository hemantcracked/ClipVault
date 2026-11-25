package com.example.xdownloader.data.remote.extractor

import com.example.xdownloader.domain.model.VideoVariant
import java.util.regex.Pattern

object ExtractorUtils {

    fun parseResolution(url: String, width: Int, height: Int): String {
        val minDim = if (width > 0 && height > 0) minOf(width, height) else 0
        if (minDim >= 1080 || url.contains("1080x") || url.contains("x1080") || url.contains("/1080/")) return "1080p (HD)"
        if (minDim >= 720 || url.contains("720x") || url.contains("x720") || url.contains("/720/")) return "720p (HD)"
        if (minDim >= 480 || url.contains("480x") || url.contains("x480") || url.contains("/480/")) return "480p (SD)"
        if (minDim >= 360 || url.contains("360x") || url.contains("x360") || url.contains("/360/")) return "360p (SD)"
        if (minDim >= 240 || url.contains("240x") || url.contains("x240") || url.contains("/240/")) return "240p (SD)"

        // Fallback: extract dimension pattern like 1280x720 from URL
        val pattern = Pattern.compile("(\\d{3,4})x(\\d{3,4})")
        val matcher = pattern.matcher(url)
        if (matcher.find()) {
            val w = matcher.group(1)?.toIntOrNull() ?: 0
            val h = matcher.group(2)?.toIntOrNull() ?: 0
            val min = minOf(w, h)
            if (min > 0) return "${min}p"
        }

        return if (minDim > 0) "${minDim}p" else "SD"
    }

    fun calculateEstimatedSizeBytes(bitrate: Int, durationMs: Long): Long {
        if (bitrate <= 0 || durationMs <= 0) return 0L
        return (bitrate.toLong() * durationMs) / 8000L
    }

    fun filterAndSortMp4Variants(variants: List<VideoVariant>): List<VideoVariant> {
        return variants
            .filter { it.url.contains(".mp4") || it.mimeType.contains("mp4") }
            .distinctBy { it.url }
            .sortedByDescending { it.bitrate }
    }
}
