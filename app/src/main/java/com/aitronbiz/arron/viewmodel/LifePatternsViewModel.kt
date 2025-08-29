package com.aitronbiz.arron.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.LifePatterns
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LifePatternsViewModel : ViewModel() {
    private val _token = MutableStateFlow(AppController.prefs.getToken().orEmpty())
    val token: StateFlow<String> = _token

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "token") {
            _token.value = AppController.prefs.getToken().orEmpty()
        }
    }

    private val _lifePatterns = MutableStateFlow<LifePatterns?>(null)
    val lifePatterns: StateFlow<LifePatterns?> = _lifePatterns

    private val _selectedDate = mutableStateOf(LocalDate.now())
    val selectedDate: State<LocalDate> = _selectedDate

    init {
        AppController.prefs.registerOnChangeListener(prefListener)
    }

    fun resetState(homeId: String) {
        _selectedDate.value = LocalDate.now()
        _lifePatterns.value = null
        fetchLifePatternsData(homeId, _selectedDate.value)
    }

    fun fetchLifePatternsData(homeId: String, selectedDate: LocalDate) {
        viewModelScope.launch {
            try {
                val tk = _token.value
                if (tk.isBlank()) {
                    _lifePatterns.value = null
                    return@launch
                }

                val startDateStr = selectedDate.toString()
                val endDateStr   = selectedDate.plusDays(1).toString()

                val res = RetrofitClient.apiService.getLifePatterns(
                    token    = "Bearer $tk",
                    homeId   = homeId,
                    startDate = startDateStr,
                    endDate   = endDateStr
                )

                if (res.isSuccessful) {
                    val patterns = res.body()?.lifePatterns.orEmpty()

                    val matched = patterns.firstOrNull { lp ->
                        val sd = lp.summaryDate
                        if (sd.isNullOrBlank()) return@firstOrNull false

                        val byInstant = runCatching {
                            Instant.parse(sd).atZone(ZoneId.systemDefault()).toLocalDate()
                        }.getOrNull() == selectedDate

                        val byLocalDate = runCatching {
                            LocalDate.parse(sd)
                        }.getOrNull() == selectedDate

                        val byPrefix = sd.startsWith(startDateStr)

                        byInstant || byLocalDate || byPrefix
                    }

                    _lifePatterns.value = matched
                } else {
                    _lifePatterns.value = null
                    Log.e(TAG, "getLifePatterns failed: code=${res.code()} body=${res.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getLifePatterns error", e)
                _lifePatterns.value = null
            }
        }
    }

    fun checkNotifications(onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tk = _token.value
                if (tk.isBlank()) {
                    withContext(Dispatchers.Main) { onResult(false) }
                    return@launch
                }
                val res = RetrofitClient.apiService.getNotification("Bearer $tk", 1, 40)
                if (res.isSuccessful) {
                    val hasUnread = res.body()?.notifications?.any { it.isRead == false } ?: false
                    withContext(Dispatchers.Main) { onResult(hasUnread) }
                } else {
                    withContext(Dispatchers.Main) { onResult(false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "checkNotifications error", e)
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    override fun onCleared() {
        AppController.prefs.unregisterOnChangeListener(prefListener)
        super.onCleared()
    }
}
