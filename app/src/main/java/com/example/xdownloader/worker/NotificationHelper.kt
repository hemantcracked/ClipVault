package com.example.xdownloader.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.example.ClipVault.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "clipvault_downloads_channel"
        const val CHANNEL_NAME = "ClipVault Downloads"
        const val NOTIFICATION_ID_BASE = 1000
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live media download progress for ClipVault"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createForegroundInfo(
        notificationId: Int,
        title: String,
        progress: Int,
        cancelIntent: PendingIntent
    ): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("ClipVault Downloading")
            .setContentText("$title ($progress%)")
            .setSmallIcon(R.drawable.ic_stat_clipvault)
            .setOngoing(true)
            .setProgress(100, progress, progress <= 0)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    fun showCompletionNotification(notificationId: Int, title: String, contentUri: Uri) {
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, if (contentUri.toString().contains("audio")) "audio/*" else "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("ClipVault Download Complete")
            .setContentText(title)
            .setSmallIcon(R.drawable.ic_stat_clipvault)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showFailureNotification(notificationId: Int, title: String, errorReason: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("ClipVault Download Failed")
            .setContentText("$title: $errorReason")
            .setSmallIcon(R.drawable.ic_stat_clipvault)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
