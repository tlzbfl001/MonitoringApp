package com.aitronbiz.arron.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.FcmTokenDTO
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.generateRandomUUID
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM New Token: $token")
        sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "알림 제목"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "알림 내용"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            "fcm_channel",
            "FCM 알림 채널",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(this, "fcm_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(0, builder.build())
    }

    companion object {
        fun sendTokenToServer(token: String) {
            val fcmTokenDTO = FcmTokenDTO(
                token = token,
                deviceId = generateRandomUUID(),
                deviceType = "android",
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.apiService.saveFcmToken(
                        token = "Bearer ${AppController.prefs.getToken()}",
                        dto = fcmTokenDTO
                    )
                    if(!response.isSuccessful) {
                        Log.e(TAG, "saveFcmToken: $response")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "saveFcmToken: $e")
                }
            }
        }
    }
}