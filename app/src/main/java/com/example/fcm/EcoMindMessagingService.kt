package com.example.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class EcoMindMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New Firebase Messaging Token refreshed: $token")
        // Save FCM token locally or publish to server/Firestore if needed
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        // Parse notification title and body
        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "🚨 Critical Sensor Alert"

        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: "An environmental sensor surpassed critical threshold safety limits."

        val sensorType = remoteMessage.data["sensorType"] ?: "ENVIRONMENTAL"
        val alertLevel = remoteMessage.data["alertLevel"] ?: "CRITICAL"

        sendPushNotification(
            context = this,
            title = title,
            body = body,
            sensorType = sensorType,
            alertLevel = alertLevel
        )
    }

    companion object {
        private const val TAG = "EcoMindFCM"
        const val CHANNEL_ID = "critical_sensor_alerts_channel"
        const val CHANNEL_NAME = "Critical Sensor Safety Alerts"
        const val PREFS_NAME = "ecomind_fcm_prefs"
        const val KEY_FCM_TOKEN = "fcm_registration_token"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "High priority notifications for critical environmental sensor threshold alerts."
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400, 200, 400)
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel $CHANNEL_ID initialized")
            }
        }

        fun sendPushNotification(
            context: Context,
            title: String,
            body: String,
            sensorType: String = "CRITICAL",
            alertLevel: String = "HIGH"
        ) {
            createNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("OPEN_TAB", "dashboard")
                putExtra("SENSOR_ALERT", sensorType)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = (System.currentTimeMillis() % 10000).toInt()
            notificationManager.notify(notificationId, notificationBuilder.build())
            Log.d(TAG, "Push Notification posted ID=$notificationId: $title - $body")
        }

        fun fetchFcmToken(context: Context? = null, onTokenRetrieved: (String?) -> Unit) {
            try {
                if (context != null && com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                    com.google.firebase.FirebaseApp.initializeApp(context)
                }
                val apps = com.google.firebase.FirebaseApp.getApps(context ?: com.google.firebase.FirebaseApp.getInstance().applicationContext)
                if (apps.isNotEmpty()) {
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                            onTokenRetrieved(null)
                            return@addOnCompleteListener
                        }
                        val token = task.result
                        Log.d(TAG, "Retrieved FCM Token: $token")
                        onTokenRetrieved(token)
                    }
                } else {
                    onTokenRetrieved(null)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "FCM token initialization: ${e.message}")
                onTokenRetrieved(null)
            }
        }
    }
}
