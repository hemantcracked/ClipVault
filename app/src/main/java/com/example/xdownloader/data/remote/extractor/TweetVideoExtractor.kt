package com.example.xdownloader.data.remote.extractor

import com.example.xdownloader.domain.model.ExtractionResult

interface TweetVideoExtractor {
    val name: String
    suspend fun extract(tweetId: String): ExtractionResult
}
