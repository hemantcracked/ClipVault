package com.example.xdownloader.domain.model

import android.net.Uri

data class WhatsAppStatusItem(
    val id: String,
    val documentUri: Uri,
    val fileName: String,
    val mediaType: MediaType,
    val lastModified: Long,
    val isSaved: Boolean = false,
    val isBusiness: Boolean = false
)
