package com.aitronbiz.arron.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class RespirationViewModel : ViewModel() {
    // 차트 데이터
    private val _chartData = MutableStateFlow<List<ChartPoint>>(emptyList())
    val chartData: StateFlow<List<ChartPoint>> = _chartData

    // 선택 날짜
    private val _selectedDate = mutableStateOf(LocalDate.now())
    val selectedDate: State<LocalDate> = _selectedDate

    // 차트에서 선택된 인덱스
    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex

    // 자동 스크롤 여부
    private val _autoScrollEnabled = MutableStateFlow(true)
    fun setAutoScrollEnabled(enabled: Boolean) { _autoScrollEnabled.value = enabled }

    private var respirationJob: Job? = null

    fun updateSelectedDate(date: LocalDate) {
        if (date.isAfter(LocalDate.now())) {
            return
        }
        _selectedDate.value = date
    }

    fun selectBar(index: Int) {
        _selectedIndex.value = index
    }

    fun fetchRespirationData(roomId: String, selectedDate: LocalDate) {
        // 이전 잡 종료
        respirationJob?.cancel()
        respirationJob = null

        val today = LocalDate.now()
        when {
            selectedDate.isAfter(today) -> {
                _chartData.value = emptyList()
                return
            }
            selectedDate.isBefore(today) -> {
                // 과거 날짜: 1회 호출
                respirationJob = viewModelScope.launch {
                    fetchAndSetOnce(roomId, selectedDate, fillToNow = false)
                }
            }
            else -> {
                // 오늘: 루프 갱신
                respirationJob = viewModelScope.launch {
                    while (isActive) {
                        fetchAndSetOnce(roomId, selectedDate, fillToNow = true)
                        delayUntilNextMinute()
                    }
                }
            }
        }
    }

    private suspend fun fetchAndSetOnce(
        roomId: String,
        selectedDate: LocalDate,
        fillToNow: Boolean
    ) {
        val token = AppController.prefs.getToken().orEmpty()
        if (token.isEmpty()) {
            _chartData.value = emptyList()
            Log.e(TAG, "fetchRespirationData: token is empty")
            return
        }

        try {
            val res = RetrofitClient.apiService.getRespiration("Bearer $token", roomId)
            if (!res.isSuccessful) {
                Log.e(TAG, "getRespiration: ${res.code()}")
                return
            }

            val breathingList = res.body()?.breathing ?: emptyList()

            val zoneId = ZoneId.systemDefault()
            val formatterHHmm = DateTimeFormatter.ofPattern("HH:mm")

            // 선택 날짜의 시작/끝
            val startOfDay = selectedDate.atStartOfDay(zoneId).toInstant()
            val endOfDay = selectedDate.plusDays(1).atStartOfDay(zoneId).toInstant()

            // 선택 날짜에 해당하는 데이터만 필터링
            val filtered = breathingList.filter { item ->
                runCatching {
                    val created = Instant.parse(item.createdAt)
                    created >= startOfDay && created < endOfDay
                }.getOrDefault(false)
            }

            // 서버 데이터 -> HH:mm 라벨로 매핑
            val mapped = filtered.map { item ->
                val timeLabel = Instant.parse(item.createdAt)
                    .atZone(zoneId)
                    .toLocalTime()
                    .truncatedTo(ChronoUnit.MINUTES)
                    .format(formatterHHmm)
                ChartPoint(timeLabel, item.breathingRate)
            }.distinctBy { it.timeLabel }
                .sortedBy { it.timeLabel }

            // 하루 슬롯 생성 (과거: 1440, 오늘: 현재분까지)
            val now = LocalTime.now()
            val endIndex = if (fillToNow) now.hour * 60 + now.minute else 1439
            val end = endIndex.coerceIn(0, 1439)

            val slots = MutableList(end + 1) { index ->
                val h = index / 60
                val m = index % 60
                val label = "%02d:%02d".format(h, m)
                ChartPoint(label, 0f)
            }

            val map = mapped.associateBy { it.timeLabel }
            val filled = slots.map { map[it.timeLabel] ?: it }

            _chartData.value = filled
        } catch (e: Exception) {
            Log.e(TAG, "fetchAndSetOnce", e)
        }
    }

    private suspend fun delayUntilNextMinute() {
        val now = System.currentTimeMillis()
        val next = ((now / 60_000) + 1) * 60_000
        delay(next - now)
    }
}
