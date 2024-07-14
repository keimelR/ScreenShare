package com.example.app_sistemas_operativos.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.app_sistemas_operativos.R

object Notification {
    // ID del canal de notificación
    private const val CHANNEL_ID = "SCREEN_CAPTURE_CHANNEL"

    /*
    *   createNotificationChannel()
    *
    *   Crea un canal de notificación para el servicio de captura de pantalla para Android 8.0 o superior.
    */
    private fun createNotificationChannel(context: Context) {
        // Verifica si la versión del dispositivo es mayor o igual a Android 8.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Crea el canal de notificación
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            // Registra el canal de notificación
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    /*
    *   buildNotification()
    *
    *   Crea una notificación para el servicio de captura de pantalla.
    */
    fun buildNotification(context: Context, title: String, text: String): android.app.Notification {
        // Verifica si la versión del dispositivo es mayor o igual a Android 8.0
        createNotificationChannel(context)

        // Construye la notificación
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.share_screen_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}