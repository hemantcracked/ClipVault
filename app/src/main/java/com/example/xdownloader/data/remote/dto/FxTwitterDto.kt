package com.example.xdownloader.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FxResponse(
    val code: Int = 200,
    val message: String = "",
    val tweet: FxTweet? = null
)

@Serializable
data class FxTweet(
    val id: String = "",
    val text: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val author: FxAuthor? = null,
    val media: FxMediaContainer? = null,
    val quote: FxTweet? = null
)

@Serializable
data class FxAuthor(
    val name: String = "",
    @SerialName("screen_name") val screenName: String = "",
    @SerialName("avatar_url") val avatarUrl: String = ""
)

@Serializable
data class FxMediaContainer(
    val videos: List<FxVideo>? = null
)

@Serializable
data class FxVideo(
    val url: String = "",
    @SerialName("thumbnail_url") val thumbnailUrl: String = "",
    val duration: Double = 0.0,
    val format: String = "",
    val type: String = "video",
    val width: Int = 0,
    val height: Int = 0,
    val variants: List<FxVariant>? = null
)

@Serializable
data class FxVariant(
    val url: String = "",
    val bitrate: Int = 0,
    @SerialName("content_type") val contentType: String = ""
)
