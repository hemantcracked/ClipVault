package com.example.xdownloader.worker

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreWriter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun createPendingVideoUri(filename: String, relativePath: String = "Movies/ClipVault/X"): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        return context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
    }

    fun createPendingImageUri(filename: String, relativePath: String = "Pictures/ClipVault"): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, if (filename.endsWith(".png", true)) "image/png" else "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        return context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
    }

    fun createPendingAudioUri(filename: String, relativePath: String = "Music/ClipVault"): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        return context.contentResolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
    }

    fun finishPendingVideoUri(uri: Uri): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            val rows = context.contentResolver.update(uri, contentValues, null, null)
            return rows > 0
        }
        return true
    }

    fun deleteUri(uri: Uri) {
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun writeStreamToUri(
        inputStream: InputStream,
        uri: Uri,
        totalBytes: Long = -1L,
        onProgress: (bytesDownloaded: Long, progressPercent: Int) -> Unit = { _, _ -> }
    ): Long {
        var outputStream: OutputStream? = null
        var bytesCopied = 0L
        val buffer = ByteArray(8192)

        try {
            outputStream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Could not open output stream for URI: $uri")

            var bytesRead: Int
            var lastReportedTime = System.currentTimeMillis()

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                bytesCopied += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastReportedTime > 300 || (totalBytes > 0 && bytesCopied == totalBytes)) {
                    lastReportedTime = now
                    val percent = if (totalBytes > 0) ((bytesCopied * 100) / totalBytes).toInt() else 0
                    onProgress(bytesCopied, percent)
                }
            }
            outputStream.flush()
            return bytesCopied
        } finally {
            outputStream?.close()
        }
    }

    fun isFileAccessible(uriString: String): Boolean {
        if (uriString.isBlank()) return false
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
