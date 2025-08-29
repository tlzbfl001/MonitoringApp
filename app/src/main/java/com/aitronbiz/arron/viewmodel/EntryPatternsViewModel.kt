package com.aitronbiz.arron.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.max

class EntryPatternsViewModel : ViewModel() {
    // 방 목록
    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms

    // 방 목록 로드
    fun fetchRooms(token: String, homeId: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.apiService.getAllRoom("Bearer $token", homeId)
                if (resp.isSuccessful) {
                    _rooms.value = resp.body()?.rooms ?: emptyList()
                } else {
                    Log.d(TAG, "getAllRoom: $resp")
                    _rooms.value = emptyList()
                }
            } catch (t: Throwable) {
                Log.e(TAG, "getAllRoom error", t)
                _rooms.value = emptyList()
            }
        }
    }

    // 1시간 단위 차트 포인트
    private val _chart = MutableStateFlow<List<EntryChartPoint>>(emptyList())
    val chart: StateFlow<List<EntryChartPoint>> = _chart

    // 합계
    private val _totalEnter = MutableStateFlow(0)
    val totalEnter: StateFlow<Int> = _totalEnter
    private val _totalExit = MutableStateFlow(0)
    val totalExit: StateFlow<Int> = _totalExit

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val zone: ZoneId = ZoneId.of("Asia/Seoul")
    private val iso: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

    fun maxY(): Int = max(1, chart.value.maxOfOrNull { it.total } ?: 1)

    fun fetchEntryPatternsForSelection(
        token: String,
        homeId: String,
        roomIdOrAll: String,
        date: LocalDate
    ) {
        if (token.isBlank() || homeId.isBlank()) {
            _chart.value = emptyList()
            _totalEnter.value = 0
            _totalExit.value = 0
            _error.value = "token/homeId is blank"
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            val dayStart = date.atStartOfDay(zone).toInstant()
            val nextDayStart = date.plusDays(1).atStartOfDay(zone).toInstant()

            try {
                val points: List<EntryChartPoint> = if (roomIdOrAll == "ALL") {
                    // 모든 방 합산
                    val roomIds = rooms.value.map { it.id }
                    if (roomIds.isEmpty()) {
                        buildZeros()
                    } else {
                        val lists = roomIds.map { id ->
                            async(Dispatchers.IO) {
                                fetchOneRoomEntryPoints(token, id, dayStart, nextDayStart)
                            }
                        }.awaitAll()
                        mergeHourly(lists)
                    }
                } else {
                    // 단일 방
                    withContext(Dispatchers.IO) {
                        fetchOneRoomEntryPoints(token, roomIdOrAll, dayStart, nextDayStart)
                    }
                }

                _chart.value = points
                _totalEnter.value = points.sumOf { it.enterCount }
                _totalExit.value = points.sumOf { it.exitCount }
            } catch (e: Exception) {
                Log.e(TAG, "fetchEntryPatternsForSelection error", e)
                _chart.value = emptyList()
                _totalEnter.value = 0
                _totalExit.value = 0
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    private suspend fun fetchOneRoomEntryPoints(
        token: String,
        roomId: String,
        start: Instant,
        end: Instant
    ): List<EntryChartPoint> {
        return try {
            val res = RetrofitClient.apiService.getEntryPatterns(
                token = "Bearer $token",
                roomId = roomId,
                startTime = iso.format(start),
                endTime = iso.format(end)
            )
            if (!res.isSuccessful) return buildZeros()

            val enters = IntArray(24)
            val exits = IntArray(24)

            res.body()?.presences.orEmpty().forEach { p ->
                runCatching {
                    val hour = Instant.parse(p.startTime).atZone(zone).hour
                    if (hour in 0..23) {
                        if (p.isPresent) enters[hour]++ else exits[hour]++
                    }
                }.onFailure {
                    Log.w(TAG, "bad startTime: ${p.startTime}", it)
                }
            }

            (0..23).map { h ->
                EntryChartPoint(
                    hourLabel = "%02d:00".format(h),
                    enterCount = enters[h],
                    exitCount = exits[h]
                )
            }
        } catch (t: Throwable) {
            Log.e(TAG, "fetchOneRoomEntryPoints error", t)
            buildZeros()
        }
    }

    private fun mergeHourly(listOfLists: List<List<EntryChartPoint>>): List<EntryChartPoint> {
        if (listOfLists.isEmpty()) return buildZeros()
        val map = LinkedHashMap<String, Pair<Int, Int>>()
        // 00~23 모든 레이블 확보
        for (h in 0..23) map["%02d:00".format(h)] = 0 to 0

        listOfLists.forEach { points ->
            points.forEach { p ->
                val prev = map[p.hourLabel] ?: (0 to 0)
                map[p.hourLabel] = (prev.first + p.enterCount) to (prev.second + p.exitCount)
            }
        }

        return map.entries.map { (lbl, pair) ->
            EntryChartPoint(lbl, pair.first, pair.second)
        }
    }

    private fun buildZeros(): List<EntryChartPoint> =
        (0..23).map { h -> EntryChartPoint("%02d:00".format(h), 0, 0) }

    fun checkNotifications(onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tk = AppController.prefs.getToken().orEmpty()
                if (tk.isNotBlank()) {
                    val response = RetrofitClient.apiService.getNotification("Bearer $tk", 1, 40)
                    if (response.isSuccessful) {
                        val notifications = response.body()?.notifications ?: emptyList()
                        val hasUnread = notifications.any { it.isRead == false }
                        withContext(Dispatchers.Main) { onResult(hasUnread) }
                    } else {
                        Log.e(TAG, "getNotification: $response")
                        withContext(Dispatchers.Main) { onResult(false) }
                    }
                } else {
                    withContext(Dispatchers.Main) { onResult(false) }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }
}

data class EntryChartPoint(
    val hourLabel: String,
    val enterCount: Int,
    val exitCount: Int
) {
    val total: Int get() = enterCount + exitCount
}
