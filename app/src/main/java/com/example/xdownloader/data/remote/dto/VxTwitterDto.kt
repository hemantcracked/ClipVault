package com.example.xdownloader.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VxResponse(
    val tweetID: String = "",
    val text: String = "",
    @SerialName("user_name") val userName: String = "",
    @SerialName("user_screen_name") val userScreenName: String = "",
    @SerialName("user_profile_image_url") val userProfileImageUrl: String = "",
    @SerialName("media_extended") val mediaExtended: List<VxMedia>? = null,
    val date: String = "",
    @SerialName("q_tweet") val qTweet: VxResponse? = null
)

@Serializable
data class VxMedia(
    val type: String = "video",
    val url: String = "",
    @SerialName("thumbnail_url") val thumbnailUrl: String = "",
    @SerialName("duration_millis") val durationMillis: Long = 0L,
    val size: VxSize? = null,
    val variants: List<VxVariant>? = null
)

@Serializable
data class VxSize(
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class VxVariant(
    val url: String = "",
    val bitrate: Int = 0,
    @SerialName("content_type") val contentType: String = ""
)
