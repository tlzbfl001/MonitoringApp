package com.aitronbiz.arron.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FallViewModel(application: Application) : AndroidViewModel(application) {
    val selectedIndex = MutableStateFlow(0)
    fun selectBar(minuteOfDay: Int) { selectedIndex.value = minuteOfDay.coerceIn(0, 1439) }

    private val _chartPoints = MutableStateFlow<List<ChartPoint>>(emptyList())
    val chartPoints: StateFlow<List<ChartPoint>> = _chartPoints

    private val _totalCount = MutableStateFlow(0)

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms

    private val _presenceByRoomId = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val presenceByRoomId: StateFlow<Map<String, Boolean>> = _presenceByRoomId

    private val _lastFallInstant = MutableStateFlow<Instant?>(null)
    val lastFallInstant: StateFlow<Instant?> = _lastFallInstant

    private val _selectedRoomId = MutableStateFlow("")
    val selectedRoomIdFlow: StateFlow<String> = _selectedRoomId

    private val _homeId = MutableStateFlow("")
    fun setHomeId(id: String) { _homeId.value = id }

    fun setSelectedRoomId(id: String) { _selectedRoomId.value = id }

    private var selectedDate: LocalDate = LocalDate.now()
    private var pollJob: Job? = null

    private val _respChartData = MutableStateFlow<List<ChartPoint>>(emptyList())
    val respChartData: StateFlow<List<ChartPoint>> = _respChartData

    private val _respSelectedIndex = MutableStateFlow(0)
    val respSelectedIndex: StateFlow<Int> = _respSelectedIndex
    fun selectRespIndex(idx: Int) { _respSelectedIndex.value = idx }

    private val _currentBpm = MutableStateFlow(0f)
    val currentBpm: StateFlow<Float> = _currentBpm

    private val _respTick = MutableStateFlow(0L)
    val respTick: StateFlow<Long> = _respTick

    // 호흡 폴링 잡은 별도로 관리
    private var respPollingJob: Job? = null

    fun updateSelectedDate(date: LocalDate) {
        selectedDate = date
        _chartPoints.value = emptyList()
        _lastFallInstant.value = null

        // 낙상/호흡 모두 정리
        stopFallPolling()
        stopRespPolling()

        // 호흡 UI 초기화
        _respChartData.value = emptyList()
        _currentBpm.value = 0f
        _respSelectedIndex.value = 0
        _respTick.value = 0L
    }

    fun fetchRooms(homeId: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.apiService.getAllRoom("Bearer ${AppController.prefs.getToken()!!}", homeId)
                val list = if (resp.isSuccessful) resp.body()?.rooms ?: emptyList() else emptyList()
                _rooms.value = list

                if (list.isNotEmpty()) {
                    preloadPresenceForRooms(list)
                }
            } catch (t: Throwable) {
                _rooms.value = emptyList()
                Log.e(TAG, "getAllRoom error", t)
            }
        }
    }

    fun fetchPresence(roomId: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.apiService.getPresence("Bearer ${AppController.prefs.getToken()!!}", roomId)
                if (resp.isSuccessful) {
                    val isPresent = resp.body()?.isPresent == true
                    _presenceByRoomId.value = _presenceByRoomId.value.toMutableMap().apply {
                        this[roomId] = isPresent
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "getPresence error", t)
            }
        }
    }

    private suspend fun preloadPresenceForRooms(rooms: List<Room>) = coroutineScope {
        val token = AppController.prefs.getToken().orEmpty()
        if (token.isBlank() || rooms.isEmpty()) return@coroutineScope

        val results = rooms.map { room ->
            async(Dispatchers.IO) {
                runCatching {
                    val resp = RetrofitClient.apiService.getPresence("Bearer $token", room.id)
                    room.id to (resp.isSuccessful && (resp.body()?.isPresent == true))
                }.getOrElse { room.id to false }
            }
        }.awaitAll()

        val map = results.toMap()
        _presenceByRoomId.value = _presenceByRoomId.value.toMutableMap().apply {
            putAll(map)
        }
    }

    fun fetchFallsData(roomId: String, date: LocalDate) {
        stopPolling()
        selectedDate = date
        _selectedRoomId.value = roomId

        pollJob = viewModelScope.launch {
            val isPast = date.isBefore(LocalDate.now())

            suspend fun runOnce() {
                if (roomId == "ALL") {
                    val rooms = _rooms.value
                    val ids = rooms.map { it.id }
                    val (points, count, lastInstant) = loadFallsForRooms(ids, date)
                    _chartPoints.value = points
                    _totalCount.value = count
                    _lastFallInstant.value = lastInstant
                } else {
                    val (points, count, lastInstant) = loadFallsForRoom(roomId, date)
                    _chartPoints.value = points
                    _totalCount.value = count
                    _lastFallInstant.value = lastInstant
                }
            }

            if (isPast) {
                runOnce()
                return@launch
            }

            while (isActive) {
                runOnce()
                val now = System.currentTimeMillis()
                val nextMinute = ((now / 60_000) + 1) * 60_000
                delay(nextMinute - now)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    // 여러 방 병합
    private suspend fun loadFallsForRooms(
        roomIds: List<String>,
        date: LocalDate
    ): Triple<List<ChartPoint>, Int, Instant?> = coroutineScope {
        val results = roomIds.map { id ->
            async { loadFallsRaw(id, date) }
        }.awaitAll()

        val allInstants = results.flatMap { it.first }
        val total = results.sumOf { it.second }
        val zone = ZoneId.systemDefault()

        val points = allInstants
            .sorted()
            .map { inst ->
                val lt = inst.atZone(zone).toLocalTime()
                ChartPoint(String.format("%02d:%02d", lt.hour, lt.minute), 1f)
            }

        val last = allInstants.maxOrNull()
        Triple(points, total, last)
    }

    // 단일 방
    private suspend fun loadFallsForRoom(
        roomId: String,
        date: LocalDate
    ): Triple<List<ChartPoint>, Int, Instant?> {
        val (instants, total) = loadFallsRaw(roomId, date)
        val zone = ZoneId.systemDefault()
        val points = instants.sorted().map { t ->
            val lt = t.atZone(zone).toLocalTime()
            ChartPoint(String.format("%02d:%02d", lt.hour, lt.minute), 1f)
        }
        val last = instants.maxOrNull()
        return Triple(points, total, last)
    }

    private suspend fun loadFallsRaw(
        roomId: String,
        date: LocalDate
    ): Pair<List<Instant>, Int> {
        val zone = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneId.of("UTC"))

        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()

        return try {
            val res = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getFalls(
                    token = "Bearer ${AppController.prefs.getToken()!!}",
                    roomId = roomId,
                    startTime = formatter.format(start),
                    endTime = formatter.format(end)
                )
            }
            if (res.isSuccessful) {
                val alerts = res.body()?.alerts.orEmpty()
                val instants = alerts.mapNotNull { a ->
                    runCatching { Instant.parse(a.detectedAt) }.getOrNull()
                }.filter { it >= start && it < end }
                instants to instants.size
            } else {
                Log.e(TAG, "getFalls: $res")
                emptyList<Instant>() to 0
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "loadFallsRaw error", e)
            emptyList<Instant>() to 0
        }
    }

    fun checkNotifications(onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!AppController.prefs.getToken().isNullOrEmpty()) {
                    val response = RetrofitClient.apiService.getNotification("Bearer ${AppController.prefs.getToken()}", 1, 40)
                    if (response.isSuccessful) {
                        val notifications = response.body()?.notifications ?: emptyList()
                        val hasUnread = notifications.any { it.isRead == false }
                        withContext(Dispatchers.Main) { onResult(hasUnread) }
                    } else {
                        withContext(Dispatchers.Main) { onResult(false) }
                    }
                } else {
                    withContext(Dispatchers.Main) { onResult(false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun fetchRespirationData(roomId: String, date: LocalDate) {
        stopRespPolling()

        val token = AppController.prefs.getToken().orEmpty()
        if (token.isBlank()) {
            clearRespUi()
            return
        }

        respPollingJob = viewModelScope.launch {
            if (date.isBefore(LocalDate.now())) {
                fetchOnceAndPublishSingle(roomId, date)
                return@launch
            }
            while (isActive) {
                fetchOnceAndPublishSingle(roomId, date)
                val now = System.currentTimeMillis()
                val nextMinute = ((now / 60_000) + 1) * 60_000
                delay(nextMinute - now)
            }
        }
    }

    fun fetchRespirationDataAll(roomIds: List<String>, date: LocalDate) {
        stopRespPolling()

        val token = AppController.prefs.getToken().orEmpty()
        if (token.isBlank() || roomIds.isEmpty()) {
            clearRespUi()
            return
        }

        respPollingJob = viewModelScope.launch {
            if (date.isBefore(LocalDate.now())) {
                fetchOnceAndPublishAll(roomIds, date)
                return@launch
            }
            while (isActive) {
                fetchOnceAndPublishAll(roomIds, date)
                val now = System.currentTimeMillis()
                val nextMinute = ((now / 60_000) + 1) * 60_000
                delay(nextMinute - now)
            }
        }
    }

    private fun stopRespPolling() {
        respPollingJob?.cancel()
        respPollingJob = null
    }

    private fun clearRespUi() {
        _respChartData.value = emptyList()
        _currentBpm.value = 0f
        _respSelectedIndex.value = 0
        _respTick.value = 0L
    }

    private suspend fun fetchOnceAndPublishSingle(roomId: String, date: LocalDate) {
        val zone = ZoneId.systemDefault()
        val (startInstant, endInstant) = boundsFor(date, zone)
        val minuteAvg = minuteAvgForRoom(roomId, startInstant, endInstant, zone)
        publishResp(minuteAvg, date)
    }

    private suspend fun fetchOnceAndPublishAll(roomIds: List<String>, date: LocalDate) {
        val zone = ZoneId.systemDefault()
        val (startInstant, endInstant) = boundsFor(date, zone)

        val roomMinuteArrays = mutableListOf<FloatArray>()
        for (rid in roomIds) {
            val arr = minuteAvgForRoom(rid, startInstant, endInstant, zone)
            roomMinuteArrays.add(arr)
        }

        val agg = FloatArray(1440) { 0f }
        for (m in 0..1439) {
            var sum = 0f
            var cnt = 0
            for (arr in roomMinuteArrays) {
                val v = arr[m]
                if (v > 0f) { sum += v; cnt++ }
            }
            agg[m] = if (cnt > 0) sum / cnt else 0f
        }

        publishResp(agg, date)
    }

    private fun boundsFor(date: LocalDate, zone: ZoneId): Pair<Instant, Instant> {
        val start = date.atStartOfDay(zone).toInstant()
        val end = if (date == LocalDate.now()) Instant.now()
        else date.plusDays(1).atStartOfDay(zone).toInstant()
        return start to end
    }

    private suspend fun minuteAvgForRoom(
        roomId: String,
        start: Instant,
        end: Instant,
        zone: ZoneId
    ): FloatArray {
        val minuteAvg = FloatArray(1440) { 0f }
        try {
            val res = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getRespiration(
                    "Bearer ${AppController.prefs.getToken()}",
                    roomId,
                    start.toString(),
                    end.toString()
                )
            }
            if (res.isSuccessful) {
                val list = res.body()?.breathing.orEmpty()
                if (list.isNotEmpty()) {
                    val buckets = Array(1440) { mutableListOf<Float>() }
                    list.forEach { b ->
                        val ts = (b.endTime.ifBlank { b.startTime })
                        val t = parseInstantFlexible(ts, zone) ?: return@forEach
                        if (t >= start && t < end) {
                            val lt = t.atZone(zone).toLocalTime()
                            val idx = lt.hour * 60 + lt.minute
                            if (idx in 0..1439) buckets[idx].add(b.breathingRate)
                        }
                    }
                    for (i in 0..1439) {
                        minuteAvg[i] = if (buckets[i].isEmpty()) 0f
                        else (buckets[i].sum() / buckets[i].size)
                    }
                }
            } else {
                Log.e(TAG, "getRespiration: $res")
            }
        } catch (e: Exception) {
            Log.e(TAG, "minuteAvgForRoom error", e)
        }
        return minuteAvg
    }

    private fun publishResp(minuteAvg: FloatArray, date: LocalDate) {
        val nowMin = if (date == LocalDate.now()) {
            val now = LocalTime.now()
            (now.hour * 60 + now.minute).coerceIn(0, 1439)
        } else 1439

        _currentBpm.value = minuteAvg[nowMin]

        _respChartData.value = List(1440) { m ->
            val h = m / 60
            val mm = m % 60
            ChartPoint(String.format("%02d:%02d", h, mm), minuteAvg[m])
        }

        _respSelectedIndex.value = nowMin
        _respTick.value = System.currentTimeMillis()
    }

    private fun parseInstantFlexible(ts: String, zone: ZoneId): Instant? {
        runCatching { return Instant.parse(ts) }
        return runCatching {
            val ldt = LocalDateTime.parse(ts, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ldt.atZone(zone).toInstant()
        }.getOrNull()
    }

    private fun stopFallPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopFallPolling()
        stopRespPolling()
    }
}
