package com.aitronbiz.arron.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.aitronbiz.arron.entity.ChartPoint
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ActivityViewModel : ViewModel() {
    private val _chartData = MutableStateFlow<List<ChartPoint>>(emptyList())
    val chartData: StateFlow<List<ChartPoint>> = _chartData

    private val _selectedIndex = MutableStateFlow(-1)
    val selectedIndex: StateFlow<Int> = _selectedIndex

    init {
        generateInitialData()
    }

    private fun generateInitialData() {
        val now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES)
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        _chartData.value = (0 until 30).map { i ->
            val time = now.minusMinutes((290 - i * 10).toLong()).format(formatter)
            ChartPoint(time, (10..60).random().toFloat())
        }
    }

    fun selectBar(index: Int) {
        _selectedIndex.value = index
    }
}
