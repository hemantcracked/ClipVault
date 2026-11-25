package com.example.xdownloader.data.remote.api

import com.example.xdownloader.data.remote.dto.FxResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface FxTwitterApi {
    @GET("status/{id}")
    suspend fun getTweetStatus(@Path("id") tweetId: String): Response<FxResponse>
}
