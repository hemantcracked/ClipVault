package com.example.xdownloader.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyndicationResponse(
    val id_str: String = "",
    val text: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val user: SyndicationUser? = null,
    val mediaDetails: List<SyndicationMediaDetail>? = null,
    @SerialName("quoted_tweet") val quotedTweet: SyndicationResponse? = null
)

@Serializable
data class SyndicationUser(
    val name: String = "",
    @SerialName("screen_name") val screenName: String = "",
    @SerialName("profile_image_url_https") val profileImageUrlHttps: String = ""
)

@Serializable
data class SyndicationMediaDetail(
    val type: String = "",
    @SerialName("media_url_https") val mediaUrlHttps: String = "",
    @SerialName("duration_millis") val durationMillis: Long = 0L,
    @SerialName("original_info") val originalInfo: SyndicationOriginalInfo? = null,
    @SerialName("video_info") val videoInfo: SyndicationVideoInfo? = null
)

@Serializable
data class SyndicationOriginalInfo(
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class SyndicationVideoInfo(
    val variants: List<SyndicationVariant>? = null
)

@Serializable
data class SyndicationVariant(
    val url: String = "",
    val bitrate: Int = 0,
    @SerialName("content_type") val contentType: String = ""
)
