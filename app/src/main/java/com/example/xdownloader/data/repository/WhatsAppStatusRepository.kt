package com.example.xdownloader.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.xdownloader.data.local.DownloadedVideoDao
import com.example.xdownloader.data.local.DownloadedVideoEntity
import com.example.xdownloader.domain.model.DownloadStatus
import com.example.xdownloader.domain.model.MediaPlatform
import com.example.xdownloader.domain.model.MediaType
import com.example.xdownloader.domain.model.WhatsAppStatusItem
import com.example.xdownloader.worker.MediaStoreWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsAppStatusRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: DownloadedVideoDao,
    private val mediaStoreWriter: MediaStoreWriter
) {

    suspend fun loadStatuses(treeUriString: String, isBusiness: Boolean): List<WhatsAppStatusItem> = withContext(Dispatchers.IO) {
        if (treeUriString.isBlank()) return@withContext emptyList()

        try {
            val treeUri = Uri.parse(treeUriString)
            val docDir = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()

            val files = docDir.listFiles()
            files.mapNotNull { file ->
                val name = file.name ?: return@mapNotNull null
                if (name == ".nomedia") return@mapNotNull null

                val lower = name.lowercase()
                val isVideo = lower.endsWith(".mp4")
                val isImage = lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                if (!isVideo && !isImage) return@mapNotNull null

                val mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE
                val statusId = "wa_${if (isBusiness) "w4b" else "std"}_$name"
                val isSavedInDb = dao.getVideoById(statusId)?.downloadStatus == DownloadStatus.COMPLETED

                WhatsAppStatusItem(
                    id = statusId,
                    documentUri = file.uri,
                    fileName = name,
                    mediaType = mediaType,
                    lastModified = file.lastModified(),
                    isSaved = isSavedInDb,
                    isBusiness = isBusiness
                )
            }.sortedByDescending { it.lastModified }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveStatus(item: WhatsAppStatusItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(item.documentUri) ?: return@withContext false
            val isVideo = item.mediaType == MediaType.VIDEO
            val relativePath = if (isVideo) "Movies/ClipVault/WhatsApp" else "Pictures/ClipVault"

            val pendingUri = if (isVideo) {
                mediaStoreWriter.createPendingVideoUri(item.fileName, relativePath)
            } else {
                mediaStoreWriter.createPendingImageUri(item.fileName, relativePath)
            } ?: return@withContext false

            inputStream.use { input ->
                mediaStoreWriter.writeStreamToUri(input, pendingUri)
            }

            mediaStoreWriter.finishPendingVideoUri(pendingUri)

            val entity = DownloadedVideoEntity(
                id = item.id,
                tweetId = item.fileName,
                authorHandle = if (item.isBusiness) "WhatsApp Business" else "WhatsApp",
                authorName = if (item.isBusiness) "WhatsApp Business Status" else "WhatsApp Status",
                caption = "Saved WhatsApp Status (${item.fileName})",
                thumbnailUrl = item.documentUri.toString(),
                resolution = if (isVideo) "HD Video" else "Photo",
                videoUrl = item.documentUri.toString(),
                fileSizeBytes = 0L,
                durationMs = 0L,
                mediaStoreUri = pendingUri.toString(),
                downloadStatus = DownloadStatus.COMPLETED,
                createdAt = System.currentTimeMillis(),
                originalTweetUrl = "",
                platform = MediaPlatform.WHATSAPP.name,
                mediaType = item.mediaType.name
            )
            dao.insertVideo(entity)
            true
        } catch (e: Exception) {
            false
        }
    }
}
