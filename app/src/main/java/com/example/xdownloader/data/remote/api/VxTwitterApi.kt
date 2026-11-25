package com.example.xdownloader.data.remote.api

import com.example.xdownloader.data.remote.dto.VxResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface VxTwitterApi {
    @GET("Twitter/status/{id}")
    suspend fun getTweetStatus(@Path("id") tweetId: String): Response<VxResponse>
}
