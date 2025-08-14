package com.aitronbiz.arron.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.LifePatterns
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LifePatternsViewModel : ViewModel() {
    private val _lifePatterns = MutableStateFlow<LifePatterns?>(null)
    val lifePatterns: StateFlow<LifePatterns?> = _lifePatterns

    private val _selectedDate = mutableStateOf(LocalDate.now())
    val selectedDate: State<LocalDate> = _selectedDate

    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun resetState(token: String, homeId: String) {
        _selectedDate.value = LocalDate.now()
        _lifePatterns.value = null
        fetchLifePatternsData(token, homeId, _selectedDate.value)
    }

    fun fetchLifePatternsData(token: String, homeId: String, selectedDate: LocalDate) {
        viewModelScope.launch {
            try {
                val zoneId = ZoneId.systemDefault()

                // 하루의 시작과 끝을 Instant로 변환
                val startInstant = selectedDate.atStartOfDay(zoneId).toInstant()
                val endInstant = selectedDate.atTime(23, 59, 59).atZone(zoneId).toInstant()

                // ISO-8601 UTC 포맷
                val formatter = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .withZone(ZoneId.of("UTC"))

                val startTimeStr = formatter.format(startInstant)
                val endTimeStr = formatter.format(endInstant)

                val res = RetrofitClient.apiService.getLifePatterns(
                    token = "Bearer $token",
                    homeId = homeId,
                    startTime = startTimeStr,
                    endTime = endTimeStr
                )

                if (res.isSuccessful) {
                    val patterns = res.body()?.lifePatterns ?: emptyList()

                    val matchedPattern = patterns.firstOrNull { lp ->
                        val utcInstant = Instant.parse(lp.summaryDate)
                        val localDate = utcInstant.atZone(zoneId).toLocalDate()
                        localDate == selectedDate
                    }

                    _lifePatterns.value = matchedPattern
                } else {
                    _lifePatterns.value = null
                    Log.e(TAG, "getLifePatterns: ${res.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _lifePatterns.value = null
            }
        }
    }
}