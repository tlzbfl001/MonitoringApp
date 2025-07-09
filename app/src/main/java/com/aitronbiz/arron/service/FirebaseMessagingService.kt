package com.aitronbiz.arron.service

import android.os.Build
import android.util.Log
import com.aitronbiz.arron.AppController
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
        Log.d(TAG, "FCM: ${remoteMessage.data}")
    }

    private fun sendTokenToServer(token: String) {
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
                if (response.isSuccessful) {
                    Log.d(TAG, "토큰 저장 완료: ${response.body()}")
                } else {
                    Log.e(TAG, "토큰 저장 실패: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "${e.message}")
            }
        }
    }
}
