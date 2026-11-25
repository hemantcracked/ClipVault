package com.example.xdownloader.data.remote.ytdlp

import android.content.Context
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "YtDlpManager"
    }

    private val initDeferred = CompletableDeferred<Boolean>()

    init {
        initAsync()
    }

    private fun initAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(context)
                FFmpeg.getInstance().init(context)
                Log.d(TAG, "YoutubeDL and FFmpeg initialized successfully.")
                initDeferred.complete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize YoutubeDL / FFmpeg: ${e.message}", e)
                initDeferred.complete(false)
            }
        }
    }

    suspend fun ensureInitialized(): Boolean {
        return initDeferred.await()
    }

    suspend fun updateYtDlp(): String = withContext(Dispatchers.IO) {
        ensureInitialized()
        try {
            val status = YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.NIGHTLY)
            status?.name ?: "Updated"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update yt-dlp: ${e.message}", e)
            "Update failed: ${e.message}"
        }
    }

    suspend fun getVersion(): String = withContext(Dispatchers.IO) {
        ensureInitialized()
        try {
            YoutubeDL.getInstance().version(context) ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
