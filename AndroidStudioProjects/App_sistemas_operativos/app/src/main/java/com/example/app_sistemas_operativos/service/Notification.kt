package com.example.app_sistemas_operativos.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.app_sistemas_operativos.R

object Notification {
    private const val CHANNEL_ID = "SCREEN_CAPTURE_CHANNEL"

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    fun buildNotification(context: Context, title: String, text: String): android.app.Notification {
        createNotificationChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.share_screen_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}