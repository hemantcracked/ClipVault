package com.example.xdownloader.domain.model

sealed class ExtractionResult {
    data class Success(val metadata: TweetMetadata) : ExtractionResult()
    data class Error(val message: String, val type: ExtractionErrorType) : ExtractionResult()
}

enum class ExtractionErrorType {
    NO_VIDEO,
    PRIVATE_OR_DELETED,
    UNSUPPORTED_TYPE,
    NETWORK_ERROR,
    RATE_LIMITED
}
