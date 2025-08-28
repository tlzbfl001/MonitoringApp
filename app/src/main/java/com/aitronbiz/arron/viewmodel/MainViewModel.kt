package com.aitronbiz.arron.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.*
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.TokenManager
import java.time.*
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.roundToInt

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    // 날짜 선택
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    // 홈 / 장소 선택
    private val _selectedHomeId = MutableStateFlow("")
    val selectedHomeId: StateFlow<String> = _selectedHomeId
    private val _selectedHomeName = MutableStateFlow("나의 홈")
    val selectedHomeName: StateFlow<String> = _selectedHomeName
    private val _selectedRoomId = MutableStateFlow<String?>(null)
    val selectedRoomId: StateFlow<String?> = _selectedRoomId

    // 홈 / 장소 목록
    private val _homes = MutableStateFlow<List<Home>>(emptyList())
    val homes: StateFlow<List<Home>> = _homes
    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms

    // 홈 단위 재실 상태 + 첫 재실 방 이름
    private val _isHomePresent = MutableStateFlow(false)
    val isHomePresent: StateFlow<Boolean> = _isHomePresent
    private val _presentRoomName = MutableStateFlow<String?>(null)
    val presentRoomName: StateFlow<String?> = _presentRoomName

    // 디바이스
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices
    val hasDevices: StateFlow<Boolean> =
        _devices.map { it.isNotEmpty() }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    // JWT 토큰
    private val _token = MutableStateFlow(AppController.prefs.getToken().orEmpty())
    val token: StateFlow<String> = _token
    private var refreshJob: Job? = null
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "token") {
            _token.value = AppController.prefs.getToken().orEmpty()
        }
    }

    // 낙상
    private var fallAlertJob: Job? = null
    private val _todayFallCountByRoomId = MutableStateFlow<Map<String, Int>>(emptyMap())
    val todayFallCountByRoomId: StateFlow<Map<String, Int>> = _todayFallCountByRoomId
    private val _dangerTodayByRoomId = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val dangerTodayByRoomId: StateFlow<Map<String, Boolean>> = _dangerTodayByRoomId

    // 활동
    private var activityWatcherJob: Job? = null
    private val _todayActivityCurrentByRoomId = MutableStateFlow<Map<String, Int>>(emptyMap())
    val todayActivityCurrentByRoomId: StateFlow<Map<String, Int>> = _todayActivityCurrentByRoomId
    private val _dangerActivityTodayByRoomId = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val dangerActivityTodayByRoomId: StateFlow<Map<String, Boolean>> = _dangerActivityTodayByRoomId
    private val ACTIVITY_THRESHOLD = 80.0

    // 호흡
    private var respirationJob: Job? = null
    private val _todayRespCurrentByRoomId = MutableStateFlow<Map<String, Int>>(emptyMap())
    val todayRespCurrentByRoomId: StateFlow<Map<String, Int>> = _todayRespCurrentByRoomId
    private val _dangerRespTodayByRoomId = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val dangerRespTodayByRoomId: StateFlow<Map<String, Boolean>> = _dangerRespTodayByRoomId
    private val RESP_THRESHOLD_BPM = 21

    // 생활/출입 패턴
    private val _lifePatterns = MutableStateFlow<LifePatterns?>(null)
    val lifePatterns: StateFlow<LifePatterns?> = _lifePatterns

    private val _entryPatternsSummary = MutableStateFlow<EntryPatternsResponse?>(null)
    val entryPatternsSummary: StateFlow<EntryPatternsResponse?> = _entryPatternsSummary
    private val _entryPatterns = MutableStateFlow<EntryPatternsResponse?>(null)
    val entryPatterns: StateFlow<EntryPatternsResponse?> = _entryPatterns

    // 홈 평균 활동도
    private val _avgActivityAllRooms = MutableStateFlow(0)
    val avgActivityAllRooms: StateFlow<Int> = _avgActivityAllRooms

    init {
        AppController.prefs.registerOnChangeListener(prefListener)
        // 토큰이 비워지면 워처 정지
        viewModelScope.launch {
            token.collect { t ->
                if (t.isBlank()) stopWatcher()
            }
        }
    }

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

    fun checkNotifications(token: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (token.isBlank()) {
                    withContext(Dispatchers.Main) { onResult(false) }
                    return@launch
                }
                val res = RetrofitClient.apiService.getNotification("Bearer $token", 1, 40)
                if (res.isSuccessful) {
                    val hasUnread = res.body()?.notifications?.any { it.isRead == false } ?: false
                    withContext(Dispatchers.Main) { onResult(hasUnread) }
                } else {
                    withContext(Dispatchers.Main) { onResult(false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "checkNotifications error", e)
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    private fun setSelectedHomeId(id: String) {
        _selectedHomeId.value = id
        val t = _token.value
        if (t.isNotBlank() && id.isNotBlank()) {
            refreshHomePresence(id, t)     // 홈 재실 상태 + 첫 재실 방
            refreshRoomsForHome(id, t)     // 방 목록 로드
        } else {
            clearRoomsState()
            _isHomePresent.value = false
            _presentRoomName.value = null
        }
    }

    fun selectHome(home: Home) {
        _selectedHomeName.value = home.name
        setSelectedHomeId(home.id)
    }

    fun refreshHomePresence(homeId: String, token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bearer = "Bearer $token"

                val roomsRes = RetrofitClient.apiService.getAllRoom(bearer, homeId)
                if (!roomsRes.isSuccessful) {
                    _isHomePresent.value = false
                    _presentRoomName.value = null
                    return@launch
                }
                val rooms = roomsRes.body()?.rooms.orEmpty()

                var firstPresentRoomName: String? = null
                for (room in rooms) {
                    val presRes = runCatching {
                        RetrofitClient.apiService.getPresence(bearer, room.id)
                    }.getOrNull()

                    val isPresent = presRes?.isSuccessful == true && (presRes.body()?.isPresent == true)
                    if (isPresent) {
                        firstPresentRoomName = room.name.ifBlank { room.id }
                        break
                    }
                }

                _isHomePresent.value = firstPresentRoomName != null
                _presentRoomName.value = firstPresentRoomName
            } catch (e: Exception) {
                Log.e(TAG, "refreshHomePresence error", e)
                _isHomePresent.value = false
                _presentRoomName.value = null
            }
        }
    }

    // 방 목록 갱신
    private fun refreshRoomsForHome(homeId: String, token: String) {
        viewModelScope.launch {
            try {
                val bearer = "Bearer $token"
                val roomsRes = RetrofitClient.apiService.getAllRoom(bearer, homeId)
                val list = if (roomsRes.isSuccessful) roomsRes.body()?.rooms.orEmpty() else emptyList()
                _rooms.value = list
                _selectedRoomId.value = list.firstOrNull()?.id
            } catch (e: Exception) {
                Log.e(TAG, "refreshRoomsForHome error", e)
                clearRoomsState()
            }
        }
    }

    private fun clearRoomsState() {
        _rooms.value = emptyList()
        _selectedRoomId.value = null
    }

    fun selectRoom(roomId: String) { _selectedRoomId.value = roomId }

    fun fetchHomes(token: String) {
        viewModelScope.launch {
            try {
                if (token.isBlank()) { _homes.value = emptyList(); return@launch }
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

    fun getDevices(token: String, homeId: String) {
        viewModelScope.launch {
            try {
                if (token.isBlank() || homeId.isBlank()) { _devices.value = emptyList(); return@launch }
                val devRes = RetrofitClient.apiService.getAllDevice(
                    "Bearer $token",
                    homeId
                )
                val deviceList = devRes.body()?.devices?.toList().orEmpty()
                _devices.value = deviceList
            } catch (e: Exception) {
                Log.e(TAG, "getAllDevice error", e)
                _devices.value = emptyList()
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
                val currentRooms = _rooms.value
                if (currentRooms.isEmpty()) { delay(60_000); continue }

                val now = Instant.now()
                val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()

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
                            val c = res.body()?.alerts.orEmpty().size
                            counts[room.id] = c
                            dangers[room.id] = c > 0
                        } else {
                            counts[room.id] = 0
                            dangers[room.id] = false
                        }
                    } catch (_: Exception) {
                        counts[room.id] = 0
                        dangers[room.id] = false
                    }
                }

                _todayFallCountByRoomId.value = counts
                _dangerTodayByRoomId.value = dangers

                val ms = System.currentTimeMillis()
                val nextMin = ((ms / 60_000) + 1) * 60_000
                delay(nextMin - ms)
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

    fun startActivityWatcher(token: String) {
        activityWatcherJob?.cancel()
        activityWatcherJob = viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val utcFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"))

            while (isActive) {
                val currentRooms = _rooms.value
                if (currentRooms.isEmpty()) { delay(60_000); continue }

                val now = Instant.now()
                val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()

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
                                .maxByOrNull {
                                    runCatching { Instant.parse(it.endTime ?: it.startTime) }
                                        .getOrElse { Instant.EPOCH }
                                }
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

    fun startRespirationWatcher(token: String) {
        respirationJob?.cancel()
        respirationJob = viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val utcFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneId.of("UTC"))

            while (isActive) {
                val currentRooms = _rooms.value
                if (currentRooms.isEmpty()) { delay(60_000); continue }

                val now = Instant.now()
                val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()

                val latestMap = mutableMapOf<String, Int>()
                val dangerMap = mutableMapOf<String, Boolean>()

                for (room in currentRooms) {
                    try {
                        val res = RetrofitClient.apiService.getRespiration(
                            token = "Bearer $token",
                            roomId = room.id,
                            startTime = utcFmt.format(todayStart),
                            endTime   = utcFmt.format(now)
                        )
                        if (res.isSuccessful) {
                            val list = res.body()?.breathing.orEmpty()
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

                val ms = System.currentTimeMillis()
                val nextMin = ((ms / 60_000) + 1) * 60_000
                delay(nextMin - ms)
            }
        }
    }

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

    fun fetchLifePatternsData(token: String, homeId: String, selectedDate: LocalDate) {
        viewModelScope.launch {
            try {
                val startDateStr = selectedDate.toString()
                val endDateStr   = selectedDate.plusDays(1).toString()

                val res = RetrofitClient.apiService.getLifePatterns(
                    token   = "Bearer $token",
                    homeId  = homeId,
                    startDate = startDateStr,
                    endDate   = endDateStr
                )

                if (res.isSuccessful) {
                    val patterns = res.body()?.lifePatterns ?: emptyList()

                    val matched = patterns.firstOrNull { lp ->
                        val parsedLocalDate = runCatching {
                            Instant.parse(lp.summaryDate).atZone(ZoneId.systemDefault()).toLocalDate()
                        }.getOrNull()

                        parsedLocalDate == selectedDate ||
                                (lp.summaryDate?.startsWith(startDateStr) == true)
                    }

                    _lifePatterns.value = matched
                } else {
                    _lifePatterns.value = null
                    Log.e(TAG, "getLifePatterns: $res")
                }
            } catch (e: Exception) {
                _lifePatterns.value = null
                Log.e(TAG, "getLifePatterns error", e)
            }
        }
    }

    fun fetchEntryPatternsData(homeId: String, date: LocalDate) {
        viewModelScope.launch {
            try {
                val token = _token.value
                if (token.isBlank() || homeId.isBlank()) {
                    _entryPatternsSummary.value = null
                    Log.e(TAG, "fetchEntryPatternsData: token/homeId is blank")
                    return@launch
                }

                val dateStr = date.toString()
                val res = RetrofitClient.apiService.getEntryPatterns(
                    token = "Bearer $token",
                    homeId = homeId,
                    date = dateStr
                )

                if (res.isSuccessful) {
                    val body = res.body()
                    _entryPatternsSummary.value = body
                    _entryPatterns.value = null
                } else {
                    _entryPatternsSummary.value = null
                    _entryPatterns.value = null
                    Log.e(TAG, "getEntryPatterns: $res")
                }
            } catch (e: Exception) {
                _entryPatternsSummary.value = null
                _entryPatterns.value = null
                Log.e(TAG, "getEntryPatterns error", e)
            }
        }
    }

    // 홈 평균 활동도 (기존 로직 유지)
    fun fetchAvgActivityForHome(homeId: String, date: LocalDate) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant()
            val end = if (date == LocalDate.now()) Instant.now()
            else date.plusDays(1).atStartOfDay(zone).toInstant()
            val fmt = DateTimeFormatter.ISO_INSTANT

            try {
                val roomsRes = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getAllRoom("Bearer ${_token.value}", homeId)
                }
                if (!roomsRes.isSuccessful) {
                    _avgActivityAllRooms.value = 0
                    return@launch
                }
                val rooms = roomsRes.body()?.rooms.orEmpty()
                if (rooms.isEmpty()) {
                    _avgActivityAllRooms.value = 0
                    return@launch
                }

                var sum = 0.0; var cnt = 0
                for (room in rooms) {
                    try {
                        val actRes = withContext(Dispatchers.IO) {
                            RetrofitClient.apiService.getActivity(
                                token = "Bearer ${_token.value}",
                                roomId = room.id,
                                startTime = fmt.format(start),
                                endTime = fmt.format(end)
                            )
                        }
                        if (actRes.isSuccessful) {
                            val list = actRes.body()?.activityScores.orEmpty()
                            for (item in list) {
                                sum += item.activityScore.toDouble()
                                cnt++
                            }
                        }
                    } catch (_: Exception) {}
                }
                _avgActivityAllRooms.value = if (cnt > 0) (sum / cnt).roundToInt() else 0
            } catch (e: Exception) {
                Log.e(TAG, "fetchAvgActivityForHome error", e)
                _avgActivityAllRooms.value = 0
            }
        }
    }

    fun stopWatcher() {
        fallAlertJob?.cancel(); fallAlertJob = null
        activityWatcherJob?.cancel(); activityWatcherJob = null
        respirationJob?.cancel(); respirationJob = null
    }

    override fun onCleared() {
        AppController.prefs.unregisterOnChangeListener(prefListener)
        super.onCleared()
    }
}
