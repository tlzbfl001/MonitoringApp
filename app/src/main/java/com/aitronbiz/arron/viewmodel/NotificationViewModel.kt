package com.aitronbiz.arron.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.SendNotificationDTO
import com.aitronbiz.arron.api.response.NotificationData
import com.aitronbiz.arron.api.response.NotificationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class NotificationViewModel : ViewModel() {
    private val _notifications = MutableStateFlow<List<NotificationData>>(emptyList())
    val notifications: StateFlow<List<NotificationData>> = _notifications.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var nextCursor: String? = ""

    fun refresh() {
        if (_loading.value) return
        _notifications.value = emptyList()
        nextCursor = ""
        _hasMore.value = true
        loadNextPage()
    }

    fun loadNextPage() {
        if (_loading.value || !_hasMore.value) return

        viewModelScope.launch {
            _loading.value = true
            try {
                val token = "Bearer ${AppController.prefs.getToken()}"
                val cursorToUse = nextCursor ?: ""
                val response: Response<NotificationResponse> =
                    RetrofitClient.apiService.getNotification(token, cursorToUse)

                if (response.isSuccessful) {
                    val body = response.body()
                    val newItems = body?.notifications.orEmpty()

                    if (newItems.isNotEmpty()) {
                        delay(1000)
                    }

                    val merged = LinkedHashMap<String, NotificationData>()
                    _notifications.value.forEach { it.id?.let { id -> merged[id] = it } }
                    newItems.forEach { it.id?.let { id -> merged[id] = it } }
                    _notifications.value = merged.values.toList()

                    nextCursor = body?.nextCursor
                    _hasMore.value = body?.hasMore == true
                } else {
                }
            } catch (_: Exception) {
            } finally {
                _loading.value = false
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
                    token = "Bearer ${AppController.prefs.getToken()}",
                    request = request
                )
                if (response.isSuccessful) refresh()
            } catch (_: Exception) { }
        }
    }

    fun markAsRead(notificationId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.readNotification(
                    "Bearer ${AppController.prefs.getToken()}",
                    notificationId
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    onSuccess()
                    refresh()
                }
            } catch (_: Exception) { }
        }
    }
}
