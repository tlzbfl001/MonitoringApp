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
                val res = RetrofitClient.apiService.getLifePatterns(
                    "Bearer $token", homeId
                )

                if (res.isSuccessful) {
                    val patterns = res.body()?.lifePatterns ?: emptyList()
                    val zoneId = ZoneId.systemDefault()

                    // summaryDate를 UTC → LocalDate 변환 후 selectedDate와 비교
                    val matchedPattern = patterns.firstOrNull { lp ->
                        val utcInstant = Instant.parse(lp.summaryDate)
                        val localDate = utcInstant.atZone(zoneId).toLocalDate()
                        localDate == selectedDate
                    }

                    _lifePatterns.value = matchedPattern
                } else {
                    _lifePatterns.value = null
                    Log.e(TAG, "API Error: ${res.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _lifePatterns.value = null
            }
        }
    }
}