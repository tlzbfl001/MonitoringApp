package com.aitronbiz.arron.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.SendNotificationDTO
import com.aitronbiz.arron.api.response.NotificationData
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val _notifications = MutableStateFlow<List<NotificationData>>(emptyList())
    val notifications: StateFlow<List<NotificationData>> = _notifications

    fun fetchNotifications(token: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getNotification("Bearer $token")
                if (response.isSuccessful) {
                    response.body()?.let {
                        _notifications.value = it.notifications
                    }
                } else {
                    Log.e(TAG, "getNotification: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getNotification: $e")
            }
        }
    }

    fun sendTestNotification(token: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = SendNotificationDTO(
                    title = "테스트 알림",
                    body = "알림",
                    data = null,
                    type = "general",
                    userId = userId
                )
                val response = RetrofitClient.apiService.sendNotification(
                    token = "Bearer $token",
                    request = request
                )
                if (response.isSuccessful) {
                    Log.d(TAG, "sendNotification: ${response.body()}")
                    // 서버 반영 완료 후 최신 알림 가져오기
                    fetchNotifications(token)
                }else {
                    Log.e(TAG, "sendNotification: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "sendNotification: $e")
            }
        }
    }

    fun markAsRead(token: String, notificationId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.readNotification("Bearer $token", notificationId)
                if (response.isSuccessful && response.body()?.success == true) {
                    onSuccess()
                    fetchNotifications(token) // 새로고침
                }else {
                    Log.e(TAG, "readNotification: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "readNotification: $e")
            }
        }
    }
}