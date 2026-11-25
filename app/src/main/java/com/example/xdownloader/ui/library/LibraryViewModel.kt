package com.example.xdownloader.ui.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xdownloader.data.local.DownloadedVideoDao
import com.example.xdownloader.data.local.DownloadedVideoEntity
import com.example.xdownloader.worker.MediaStoreWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val dao: DownloadedVideoDao,
    private val mediaStoreWriter: MediaStoreWriter
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val selectedPlatform = MutableStateFlow("ALL")

    val videos: StateFlow<List<DownloadedVideoEntity>> = combine(searchQuery, selectedPlatform) { query, platform ->
        Pair(query, platform)
    }.flatMapLatest { (query, platform) ->
        if (query.isNotBlank()) {
            dao.searchVideosFlow(query.trim())
        } else {
            dao.getCompletedVideosByPlatformFlow(platform)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteVideo(video: DownloadedVideoEntity) {
        viewModelScope.launch {
            if (video.mediaStoreUri.isNotBlank()) {
                try {
                    mediaStoreWriter.deleteUri(Uri.parse(video.mediaStoreUri))
                } catch (e: Exception) {
                    // Ignore
                }
            }
            dao.deleteVideoById(video.id)
        }
    }

    fun shareVideoFile(context: Context, video: DownloadedVideoEntity) {
        if (video.mediaStoreUri.isBlank()) {
            Toast.makeText(context, "File URI not available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = Uri.parse(video.mediaStoreUri)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (video.mediaType == "IMAGE") "image/*" else "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share media file", Toast.LENGTH_SHORT).show()
        }
    }

    fun openOriginalTweet(context: Context, video: DownloadedVideoEntity) {
        val url = video.originalTweetUrl.ifBlank {
            "https://x.com/${video.authorHandle}/status/${video.tweetId}"
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
        }
    }
}
