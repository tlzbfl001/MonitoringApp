package com.aitronbiz.arron.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.util.TokenManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    private val _selectedDate = MutableLiveData(LocalDate.now())
    val selectedDate: LiveData<LocalDate> = _selectedDate

    private val _selectedHomeId = MutableStateFlow("")
    val selectedHomeId: StateFlow<String> = _selectedHomeId

    private var refreshJob: Job? = null

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
}