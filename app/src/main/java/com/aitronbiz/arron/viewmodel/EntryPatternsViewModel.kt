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

class EntryPatternsViewModel : ViewModel() {
    private val _entryPatterns = MutableStateFlow<LifePatterns?>(null)
    val entryPatterns: StateFlow<LifePatterns?> = _entryPatterns

    private val _selectedDate = mutableStateOf(LocalDate.now())
    val selectedDate: State<LocalDate> = _selectedDate

    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun resetState(token: String, homeId: String) {
        _selectedDate.value = LocalDate.now()
        _entryPatterns.value = null
        fetchEntryPatternsData(token, homeId, _selectedDate.value)
    }

    fun fetchEntryPatternsData(token: String, homeId: String, selectedDate: LocalDate) {
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

                    _entryPatterns.value = matchedPattern
                } else {
                    _entryPatterns.value = null
                    Log.e(TAG, "getLifePatterns: ${res.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _entryPatterns.value = null
            }
        }
    }
}