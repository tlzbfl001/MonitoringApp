package com.aitronbiz.arron.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Home
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.TokenManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.*

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _selectedHomeId = MutableStateFlow("")
    private val _selectedHomeName = MutableStateFlow("나의 홈")
    val selectedHomeName: String get() = _selectedHomeName.value

    private val _selectedRoomId = MutableStateFlow<String?>(null)
    val selectedRoomId: String? get() = _selectedRoomId.value

    private val _homes = MutableStateFlow<List<Home>>(emptyList())
    val homes: StateFlow<List<Home>> = _homes

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms

    private val _presenceByRoomId = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val presenceByRoomId: StateFlow<Map<String, Boolean>> = _presenceByRoomId

    private val _isAnyPresent = MutableStateFlow(false)

    private var refreshJob: Job? = null

    // ──────────── 낙상 감지 ────────────
    private var fallAlertJob: Job? = null
    private val _todayFallCountByRoomId = MutableStateFlow<Map<String, Int>>(emptyMap())
    val todayFallCountByRoomId: StateFlow<Map<String, Int>> = _todayFallCountByRoomId

    private val _dangerTodayByRoomId = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val dangerTodayByRoomId: StateFlow<Map<String, Boolean>> = _dangerTodayByRoomId
    // ───────────────────────────────────────────────

    // ──────────── 활동량 감지 ─────────
    private var activityWatcherJob: Job? = null
    private val _todayActivityCurrentByRoomId = MutableStateFlow<Map<String, Int>>(emptyMap())
    val todayActivityCurrentByRoomId: StateFlow<Map<String, Int>> = _todayActivityCurrentByRoomId

    private val _dangerActivityTodayByRoomId = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val dangerActivityTodayByRoomId: StateFlow<Map<String, Boolean>> = _dangerActivityTodayByRoomId

    private val ACTIVITY_THRESHOLD = 80.0
    // ───────────────────────────────────────────────

    // ──────────── 호흡 감지 ─────────
    private var respirationJob: Job? = null
    private val _todayRespCurrentByRoomId = MutableStateFlow<Map<String, Int>>(emptyMap())
    val todayRespCurrentByRoomId: StateFlow<Map<String, Int>> = _todayRespCurrentByRoomId
    private val _dangerRespTodayByRoomId = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val dangerRespTodayByRoomId: StateFlow<Map<String, Boolean>> = _dangerRespTodayByRoomId
    private val RESP_THRESHOLD_BPM = 21
    // ───────────────────────────────────────────────

    fun startTokenRefresh(onSessionExpired: suspend () -> Unit) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            while (isActive) {
                TokenManager.checkAndRefreshJwtToken(context, onSessionExpired)
                delay(5 * 60 * 1000L)
            }
        }
    }

    fun stopTokenAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    private fun setSelectedHomeId(id: String) {
        _selectedHomeId.value = id
        val token = AppController.prefs.getToken()
        if (!token.isNullOrEmpty() && id.isNotBlank()) {
            refreshPresenceForHome(token, id)
        } else {
            clearPresenceState()
        }
    }

    fun selectHome(home: Home) {
        _selectedHomeName.value = home.name
        setSelectedHomeId(home.id)
    }

    fun selectRoom(roomId: String) { _selectedRoomId.value = roomId }

    fun fetchHomes(token: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getAllHome("Bearer $token")
                if (response.isSuccessful) {
                    _homes.value = response.body()?.homes ?: emptyList()
                } else {
                    Log.e(TAG, "getAllHome: $response")
                    _homes.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "getAllHome: $e")
                _homes.value = emptyList()
            }
        }
    }

    private fun refreshPresenceForHome(token: String, homeId: String) {
        viewModelScope.launch {
            try {
                val devRes = RetrofitClient.apiService.getAllDevice("Bearer $token", homeId)
                val roomIds = devRes.body()?.devices
                    ?.mapNotNull { it.roomId.takeIf { id -> id.isNotBlank() } }
                    ?.distinct()
                    .orEmpty()

                if (roomIds.isEmpty()) {
                    clearPresenceState()
                    return@launch
                }

                val roomCalls = roomIds.map { id ->
                    async(Dispatchers.IO) {
                        runCatching {
                            id to (RetrofitClient.apiService.getRoom("Bearer $token", id).body()?.room)
                        }.getOrElse { throwable ->
                            Log.e(TAG, "getRoom($id) error", throwable)
                            id to null
                        }
                    }
                }
                val presenceCalls = roomIds.map { id ->
                    async(Dispatchers.IO) {
                        runCatching {
                            id to (RetrofitClient.apiService.getPresence("Bearer $token", id).body()?.isPresent ?: false)
                        }.getOrElse { throwable ->
                            Log.e(TAG, "getPresence($id) error", throwable)
                            id to false
                        }
                    }
                }

                val roomMap = roomCalls.awaitAll()
                    .mapNotNull { (id, room) -> room?.copy(id = id) }
                    .associateBy { it.id }

                val presenceMap = presenceCalls.awaitAll().toMap()
                val orderedRooms = roomIds.mapNotNull { roomMap[it] }
                val anyPresent = presenceMap.values.any { it }

                _rooms.value = orderedRooms
                _presenceByRoomId.value = presenceMap
                _isAnyPresent.value = anyPresent

                val firstPresent = presenceMap.entries.firstOrNull { it.value }?.key
                _selectedRoomId.value = firstPresent ?: orderedRooms.firstOrNull()?.id
            } catch (e: Exception) {
                Log.e(TAG, "refreshPresenceForHome error", e)
                clearPresenceState()
            }
        }
    }

    private fun clearPresenceState() {
        _rooms.value = emptyList()
        _presenceByRoomId.value = emptyMap()
        _isAnyPresent.value = false
        _selectedRoomId.value = null
    }

    fun checkNotifications(onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = AppController.prefs.getToken()
                if (!token.isNullOrEmpty()) {
                    val response = RetrofitClient.apiService.getNotification("Bearer $token", 1, 50)
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

    fun startFallAlertWatcher(token: String) {
        fallAlertJob?.cancel()
        fallAlertJob = viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val utcFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneId.of("UTC"))

            while (isActive) {
                val now = Instant.now()
                val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()

                val currentRooms = _rooms.value
                val counts = mutableMapOf<String, Int>()
                val dangers = mutableMapOf<String, Boolean>()

                for (room in currentRooms) {
                    try {
                        val res = RetrofitClient.apiService.getFalls(
                            token = "Bearer $token",
                            roomId = room.id,
                            startTime = utcFmt.format(todayStart),
                            endTime   = utcFmt.format(now)
                        )
                        if (res.isSuccessful) {
                            val events = res.body()?.alerts.orEmpty()
                            val c = events.size
                            counts[room.id] = c
                            dangers[room.id] = c > 0
                        } else {
                            counts[room.id] = 0
                            dangers[room.id] = false
                        }
                    } catch (e: Exception) {
                        counts[room.id] = 0
                        dangers[room.id] = false
                    }
                }

                _todayFallCountByRoomId.value = counts
                _dangerTodayByRoomId.value = dangers

                // 다음 분 시작까지 대기
                val ms = System.currentTimeMillis()
                val nextMin = ((ms / 60_000) + 1) * 60_000
                delay(nextMin - ms)
            }
        }
    }

    fun startActivityWatcher(token: String) {
        activityWatcherJob?.cancel()
        activityWatcherJob = viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val utcFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"))

            while (isActive) {
                val now = Instant.now()
                val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()

                val currentRooms = _rooms.value
                val latestMap = mutableMapOf<String, Int>()
                val dangerMap = mutableMapOf<String, Boolean>()

                for (room in currentRooms) {
                    try {
                        val res = RetrofitClient.apiService.getActivity(
                            token = "Bearer $token",
                            roomId = room.id,
                            startTime = utcFmt.format(todayStart),
                            endTime   = utcFmt.format(now)
                        )
                        if (res.isSuccessful) {
                            val scores = res.body()?.activityScores.orEmpty()
                            val lastVal = scores
                                .maxByOrNull { runCatching { Instant.parse(it.endTime ?: it.startTime) }.getOrElse { Instant.EPOCH } }
                                ?.activityScore?.toDouble() ?: 0.0
                            val iv = lastVal.toInt().coerceAtLeast(0)
                            latestMap[room.id] = iv
                            dangerMap[room.id] = lastVal >= ACTIVITY_THRESHOLD
                        } else {
                            latestMap[room.id] = 0
                            dangerMap[room.id] = false
                        }
                    } catch (_: Exception) {
                        latestMap[room.id] = 0
                        dangerMap[room.id] = false
                    }
                }

                _todayActivityCurrentByRoomId.value = latestMap
                _dangerActivityTodayByRoomId.value = dangerMap

                val ms = System.currentTimeMillis()
                val nextMin = ((ms / 60_000) + 1) * 60_000
                delay(nextMin - ms)
            }
        }
    }

    suspend fun getActivityAvgForDateAcrossHome(token: String, homeId: String, date: LocalDate): Int {
        return withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            val utcFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"))

            try {
                val roomsResp = RetrofitClient.apiService.getAllRoom("Bearer $token", homeId)
                if (!roomsResp.isSuccessful) return@withContext 0
                val roomList = roomsResp.body()?.rooms ?: emptyList()

                if (roomList.isEmpty()) return@withContext 0

                val allValues = coroutineScope {
                    roomList.map { room ->
                        async {
                            try {
                                val res = RetrofitClient.apiService.getActivity(
                                    token = "Bearer $token",
                                    roomId = room.id,
                                    startTime = utcFmt.format(start),
                                    endTime   = utcFmt.format(end)
                                )
                                if (res.isSuccessful) {
                                    res.body()?.activityScores
                                        ?.mapNotNull { it.activityScore?.toDouble() }
                                        ?.filter { it > 0.0 }
                                        .orEmpty()
                                } else emptyList()
                            } catch (_: Exception) {
                                emptyList()
                            }
                        }
                    }.awaitAll().flatten()
                }

                if (allValues.isEmpty()) 0 else allValues.average().toInt().coerceAtLeast(0)
            } catch (e: Exception) {
                Log.e(TAG, "getActivityAvgForDateAcrossHome error", e)
                0
            }
        }
    }

    suspend fun getFallTotalForDateAcrossHome(
        token: String,
        homeId: String,
        date: LocalDate
    ): Int = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val utcFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"))

        try {
            val roomsResp = RetrofitClient.apiService.getAllRoom("Bearer $token", homeId)
            if (!roomsResp.isSuccessful) return@withContext 0
            val roomList = roomsResp.body()?.rooms ?: emptyList()
            if (roomList.isEmpty()) return@withContext 0

            coroutineScope {
                roomList.map { room ->
                    async {
                        try {
                            val res = RetrofitClient.apiService.getFalls(
                                token = "Bearer $token",
                                roomId = room.id,
                                startTime = utcFmt.format(start),
                                endTime   = utcFmt.format(end)
                            )
                            if (res.isSuccessful) res.body()?.alerts?.size ?: 0 else 0
                        } catch (_: Exception) {
                            0
                        }
                    }
                }.awaitAll().sum()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getFallTotalForDateAcrossHome error", e)
            0
        }
    }

    fun startRespirationWatcher(token: String) {
        respirationJob?.cancel()
        respirationJob = viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val utcFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneId.of("UTC"))

            while (isActive) {
                val now = Instant.now()
                val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()

                val latestMap = mutableMapOf<String, Int>()
                val dangerMap = mutableMapOf<String, Boolean>()

                for (room in _rooms.value) {
                    try {
                        val res = RetrofitClient.apiService.getRespiration(
                            token = "Bearer $token",
                            roomId = room.id,
                            startTime = utcFmt.format(todayStart),
                            endTime   = utcFmt.format(now)
                        )
                        if (res.isSuccessful) {
                            val list = res.body()?.breathing.orEmpty()
                            // endTime(또는 startTime) 기준 가장 최신 샘플 선택
                            val latest = list.maxByOrNull { b ->
                                runCatching {
                                    Instant.parse(b.endTime.ifBlank { b.startTime })
                                }.getOrDefault(Instant.EPOCH)
                            }
                            val bpm = (latest?.breathingRate ?: 0f).toInt().coerceAtLeast(0)
                            latestMap[room.id] = bpm
                            dangerMap[room.id] = bpm > RESP_THRESHOLD_BPM
                        } else {
                            latestMap[room.id] = 0
                            dangerMap[room.id] = false
                        }
                    } catch (_: Exception) {
                        latestMap[room.id] = 0
                        dangerMap[room.id] = false
                    }
                }

                _todayRespCurrentByRoomId.value = latestMap
                _dangerRespTodayByRoomId.value = dangerMap

                // 다음 분 시작까지 대기
                val ms = System.currentTimeMillis()
                val nextMin = ((ms / 60_000) + 1) * 60_000
                delay(nextMin - ms)
            }
        }
    }

    // 과거 호흡 평균
    suspend fun getRespAvgForDateAcrossHome(token: String, homeId: String, date: LocalDate): Int {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val utcFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneId.of("UTC"))

        return withContext(Dispatchers.IO) {
            try {
                val roomsRes = RetrofitClient.apiService.getAllRoom("Bearer $token", homeId)
                if (!roomsRes.isSuccessful) return@withContext 0
                val roomIds = roomsRes.body()?.rooms?.map { it.id }.orEmpty()
                if (roomIds.isEmpty()) return@withContext 0

                val allRates = coroutineScope {
                    roomIds.map { roomId ->
                        async {
                            try {
                                val res = RetrofitClient.apiService.getRespiration(
                                    token = "Bearer $token",
                                    roomId = roomId,
                                    startTime = utcFmt.format(start),
                                    endTime   = utcFmt.format(end)
                                )
                                if (res.isSuccessful) {
                                    res.body()?.breathing
                                        ?.map { it.breathingRate }
                                        ?.filter { it > 0f }
                                        ?.map { it.toDouble() }
                                        .orEmpty()
                                } else emptyList()
                            } catch (_: Exception) {
                                emptyList()
                            }
                        }
                    }.awaitAll().flatten()
                }

                if (allRates.isEmpty()) 0 else allRates.average().toInt()
            } catch (e: Exception) {
                Log.e(TAG, "getRespAvgForDateAcrossHome error", e)
                0
            }
        }
    }

    fun stopWatcher() {
        fallAlertJob?.cancel()
        fallAlertJob = null
        activityWatcherJob?.cancel()
        activityWatcherJob = null
        respirationJob?.cancel(); respirationJob = null
    }
}
