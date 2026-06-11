package com.voiceai.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.voiceai.assistant.R
import com.voiceai.assistant.ui.MainActivity

/**
 * VoiceListenerService: Keeps the assistant alive in the background.
 * Shows a persistent notification. Does NOT send any data externally.
 * All processing stays on-device.
 */
class VoiceListenerService : Service() {

    companion object {
        const val CHANNEL_ID = "voiceai_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.voiceai.assistant.STOP_SERVICE"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Voice AI Assistant",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Aria is running — all processing is on-device"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, VoiceListenerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aria — On-Device Assistant")
            .setContentText("🔒 Running privately. No data leaves your phone.")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_mic, "Stop", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
