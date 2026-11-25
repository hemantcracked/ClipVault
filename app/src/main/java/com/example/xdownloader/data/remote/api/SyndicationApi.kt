package com.example.xdownloader.data.remote.api

import com.example.xdownloader.data.remote.dto.SyndicationResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SyndicationApi {
    @GET("tweet-result")
    suspend fun getTweetResult(
        @Query("id") tweetId: String,
        @Query("lang") lang: String = "en"
    ): Response<SyndicationResponse>
}
