package com.example.xdownloader.di

import com.example.xdownloader.data.remote.api.FxTwitterApi
import com.example.xdownloader.data.remote.api.SyndicationApi
import com.example.xdownloader.data.remote.api.VxTwitterApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .header("Accept", "application/json, text/plain, */*")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideFxTwitterApi(okHttpClient: OkHttpClient, json: Json): FxTwitterApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.fxtwitter.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(FxTwitterApi::class.java)
    }

    @Provides
    @Singleton
    fun provideVxTwitterApi(okHttpClient: OkHttpClient, json: Json): VxTwitterApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.vxtwitter.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(VxTwitterApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSyndicationApi(okHttpClient: OkHttpClient, json: Json): SyndicationApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://cdn.syndication.twimg.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(SyndicationApi::class.java)
    }
}
