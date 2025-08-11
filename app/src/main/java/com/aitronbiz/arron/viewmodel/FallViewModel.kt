package com.aitronbiz.arron.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.model.ChartPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class FallViewModel : ViewModel() {
    private val _chartData = MutableStateFlow<List<ChartPoint>>(emptyList())
    val chartData: StateFlow<List<ChartPoint>> = _chartData

    private val _selectedDate = mutableStateOf(LocalDate.now())
    val selectedDate: State<LocalDate> = _selectedDate

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex

    // 재실 상태
    private val _isPresent = mutableStateOf<Boolean?>(null)
    val isPresent: State<Boolean?> = _isPresent

    fun selectBar(index: Int) {
        _selectedIndex.value = index
    }

    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun fetchPresence(token: String, roomId: String) {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.apiService.getPresence("Bearer $token", roomId)
                _isPresent.value = if (res.isSuccessful) res.body()?.isPresent else null
            } catch (_: Exception) {
                _isPresent.value = null
            }
        }
    }
}
