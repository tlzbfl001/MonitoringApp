package com.aitronbiz.arron.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.MainActivity
import com.aitronbiz.arron.R
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.time.LocalDateTime

class FirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // notification payload가 있으면
        remoteMessage.notification?.let {
            val title = it.title ?: "알림"
            val message = it.body ?: "알림 메시지"
            sendNotification(title, message)
        }

        // data payload가 있으면 (옵션)
        remoteMessage.data.isNotEmpty().let {
            val title = remoteMessage.data["title"] ?: "알림"
            val message = remoteMessage.data["body"] ?: "알림 메시지"
            sendNotification(title, message)
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val channelId = "notification_channel"
        val channelName = "Default Channel"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "새 토큰: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        val firestore = FirebaseFirestore.getInstance()
        val userId = if(AppController.prefs.getEmail() == null) {
            AppController.prefs.getUID().toString()
        } else AppController.prefs.getEmail()

        val data = hashMapOf(
            "token" to token,
            "updatedAt" to System.currentTimeMillis(),
            "updatedAtStr" to LocalDateTime.now().toString()
        )

        if(userId != null) {
            firestore.collection("fcmTokens")
                .document(userId)
                .set(data)
                .addOnSuccessListener {
                    Log.d(TAG, "새 토큰 Firestore 저장 성공")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "새 토큰 Firestore 저장 실패", e)
                }
        }else {
            Log.e(TAG, "새 토큰 Firestore 저장 실패")
        }
    }
}
