package com.aitronbiz.arron.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random

class FallViewModel(application: Application) : AndroidViewModel(application) {
    val selectedIndex = MutableStateFlow(0)
    fun selectBar(minuteOfDay: Int) { selectedIndex.value = minuteOfDay }

    private val _chartPoints = MutableStateFlow<List<ChartPoint>>(emptyList())
    val chartPoints: StateFlow<List<ChartPoint>> = _chartPoints

    var roomName = mutableStateOf("")
        private set

    var isPresent = mutableStateOf<Boolean?>(null)
        private set

    private var selectedDate: LocalDate = LocalDate.now()
    fun updateSelectedDate(date: LocalDate) {
        selectedDate = date
    }

    fun fetchRoomName(token: String, roomId: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.apiService.getRoom(
                    token = "Bearer $token", roomId
                )
                if (resp.isSuccessful) {
                    val name = resp.body()?.room?.name.orEmpty()
                    roomName.value = if (name.isNotBlank()) name else "방"
                } else {
                    roomName.value = "방"
                    Log.e(TAG, "getRoom: ${resp.code()}")
                }
            } catch (t: Throwable) {
                roomName.value = "방"
                Log.e(TAG, "getRoom", t)
            }
        }
    }

    fun fetchPresence(token: String, roomId: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.apiService.getPresence(
                    token = "Bearer $token",
                    roomId = roomId
                )
                if (resp.isSuccessful) {
                    isPresent.value = resp.body()?.isPresent
                } else {
                    isPresent.value = null
                    Log.e(TAG, "getPresence: ${resp.code()}")
                }
            } catch (t: Throwable) {
                isPresent.value = null
                Log.e(TAG, "getPresence", t)
            }
        }
    }

    fun fetchFallChart(token: String, roomId: String, date: LocalDate) {
        viewModelScope.launch {
            fetchFallChartInternal(roomId, date)
        }
    }

    // 데모 차트 생성(실제 API 연결 시 교체)
    private fun fetchFallChartInternal(roomId: String, date: LocalDate) {
        val seed = (roomId.hashCode() * 31 + date.toEpochDay()).toInt()
        val rand = Random(seed)
        val pointsCount = rand.nextInt(5, 17)
        val minutes = (0 until 1440).shuffled(rand).take(pointsCount).sorted()
        _chartPoints.value = minutes.map { m ->
            val h = m / 60
            val mm = m % 60
            ChartPoint("%02d:%02d".format(h, mm), 1f)
        }
    }
}