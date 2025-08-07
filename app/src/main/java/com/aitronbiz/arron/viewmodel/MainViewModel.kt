package com.aitronbiz.arron.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Home
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _selectedHomeId = MutableStateFlow("")
    val selectedHomeId: StateFlow<String> = _selectedHomeId

    private var refreshJob: Job? = null

    var homes by mutableStateOf<List<Home>>(emptyList())

    var selectedHomeName by mutableStateOf("나의 홈")

    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setSelectedHomeId(id: String) {
        _selectedHomeId.value = id
    }

    fun startTokenRefresh(onSessionExpired: suspend () -> Unit) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            while (isActive) {
                TokenManager.checkAndRefreshJwtToken(context, onSessionExpired)
                delay(5 * 60 * 1000L) // 5분 대기
            }
        }
    }

    fun stopTokenAutoRefresh() {
        refreshJob?.cancel()
    }

    fun fetchHomes(token: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getAllHome("Bearer $token")
                if (response.isSuccessful) {
                    homes = response.body()?.homes ?: emptyList()
                }else {
                    Log.e(TAG, "getAllHome: ${response.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectHome(home: Home) {
        selectedHomeName = home.name
    }

    // 알림 읽음 여부 확인
    fun checkNotifications(onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = AppController.prefs.getToken()
                if (!token.isNullOrEmpty()) {
                    val response = RetrofitClient.apiService.getNotification("Bearer $token", 1, 50)
                    if (response.isSuccessful) {
                        val notifications = response.body()?.notifications ?: emptyList()
                        val hasUnread = notifications.any { it.isRead == false }
                        withContext(Dispatchers.Main) { onResult(hasUnread) }
                    } else {
                        withContext(Dispatchers.Main) { onResult(false) }
                    }
                } else {
                    withContext(Dispatchers.Main) { onResult(false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }
}