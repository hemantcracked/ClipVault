package com.example.xdownloader.domain.model

data class TweetMetadata(
    val tweetId: String,
    val authorHandle: String,
    val authorName: String,
    val authorAvatar: String,
    val text: String,
    val createdAt: String,
    val mediaItems: List<VideoMediaItem>,
    val originalUrl: String
)

data class VideoMediaItem(
    val id: String,
    val thumbnailUrl: String,
    val variants: List<VideoVariant>,
    val durationMs: Long = 0L,
    val isGif: Boolean = false
)

data class VideoVariant(
    val url: String,
    val bitrate: Int,
    val resolution: String,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val estimatedSizeBytes: Long = 0L
)
