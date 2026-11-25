package com.example.xdownloader.data.local

import androidx.room.TypeConverter
import com.example.xdownloader.domain.model.DownloadStatus

class Converters {
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toDownloadStatus(status: String): DownloadStatus {
        return try {
            DownloadStatus.valueOf(status)
        } catch (e: Exception) {
            DownloadStatus.FAILED
        }
    }
}
