package com.example.nfc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class NfcRfidBackgroundService : Service() {

    private val binder = LocalBinder()
    private val TAG = "NfcBackgroundService"

    inner class LocalBinder : Binder() {
        fun getService(): NfcRfidBackgroundService = this@NfcRfidBackgroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NfcRfidBackgroundService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createServiceNotification("Active - Listening for RFID/NFC Environmental Tags")
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Could not start foreground service", e)
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NFC RFID Sensor Scanner",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors physical NFC/RFID sensor tags and links telemetry to Firestore locations"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createServiceNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NFC Environmental Sensor Listener")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "nfc_rfid_sensor_channel"
        const val NOTIFICATION_ID = 8842
        const val ACTION_START = "com.example.nfc.action.START_SERVICE"
        const val ACTION_STOP = "com.example.nfc.action.STOP_SERVICE"
    }
}
