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
        if (key == "token") _token.value = AppController.prefs.getToken().orEmpty()
    }

    // 낙상
    private val _lastFallTimeLabel = MutableStateFlow("-")
    val lastFallTimeLabel: StateFlow<String> = _lastFallTimeLabel
    private var lastFallWatchJob: Job? = null

    // 활동
    private var activityWatcherJob: Job? = null
    private val _todayActivityCurrentByRoomId = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _dangerActivityTodayByRoomId = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val _todayLatestActivityScore = MutableStateFlow(0)
    val todayLatestActivityScore: StateFlow<Int> = _todayLatestActivityScore
    private val ACTIVITY_THRESHOLD = 80.0

    // 홈 평균 활동도
    private val _avgActivityAllRooms = MutableStateFlow(0)
    val avgActivityAllRooms: StateFlow<Int> = _avgActivityAllRooms

    // 호흡
    private val _respNowBpm = MutableStateFlow(0)
    val respNowBpm: StateFlow<Int> = _respNowBpm
    private var respNowWatcherJob: Job? = null
    private val RESP_THRESHOLD_BPM = 21

    // 생활/출입 패턴
    private val _lifePatterns = MutableStateFlow<LifePatterns?>(null)
    val lifePatterns: StateFlow<LifePatterns?> = _lifePatterns

    // 출입 패턴
    private val _entryTotalEnter = MutableStateFlow(0)
    val entryTotalEnter: StateFlow<Int> = _entryTotalEnter
    private val _entryTotalExit = MutableStateFlow(0)
    val entryTotalExit: StateFlow<Int> = _entryTotalExit

    init {
        AppController.prefs.registerOnChangeListener(prefListener)
        viewModelScope.launch {
            token.collect { t -> if (t.isBlank()) stopWatcher() }
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

    fun updateSelectedDate(date: LocalDate) { _selectedDate.value = date }

    private fun setSelectedHomeId(id: String) {
        _selectedHomeId.value = id
        val t = _token.value
        if (t.isNotBlank() && id.isNotBlank()) {
            refreshHomePresence(id, t)
            refreshRoomsForHome(id, t)
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
                _homes.value = if (response.isSuccessful) response.body()?.homes ?: emptyList() else emptyList()
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
                val devRes = RetrofitClient.apiService.getAllDevice("Bearer $token", homeId)
                _devices.value = devRes.body()?.devices?.toList().orEmpty()
            } catch (e: Exception) {
                Log.e(TAG, "getAllDevice error", e)
                _devices.value = emptyList()
            }
        }
    }

    fun startFallWatcher(token: String, homeId: String, date: LocalDate) {
        lastFallWatchJob?.cancel()

        if (token.isBlank() || homeId.isBlank()) {
            _lastFallTimeLabel.value = "-"
            return
        }

        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val endForPast = date.plusDays(1).atStartOfDay(zone).toInstant()

        if (date.isBefore(LocalDate.now())) {
            lastFallWatchJob = viewModelScope.launch {
                val last = loadLastFallInstantAcrossRooms(token, homeId, start, endForPast)
                _lastFallTimeLabel.value = formatFallTimeLabel(last, date)
            }
            return
        }

        lastFallWatchJob = viewModelScope.launch {
            while (isActive) {
                val end = Instant.now()
                val last = loadLastFallInstantAcrossRooms(token, homeId, start, end)
                _lastFallTimeLabel.value = formatFallTimeLabel(last, date)

                val now = System.currentTimeMillis()
                val nextMin = ((now / 60_000) + 1) * 60_000
                delay(nextMin - now)
            }
        }
    }

    private fun formatFallTimeLabel(inst: Instant?, date: LocalDate): String {
        if (inst == null) return "-"
        val lt = inst.atZone(ZoneId.systemDefault()).toLocalDateTime()
        return if (lt.toLocalDate() == date) String.format("%02d:%02d", lt.hour, lt.minute) else "-"
    }

    private suspend fun loadLastFallInstantAcrossRooms(
        token: String,
        homeId: String,
        start: Instant,
        end: Instant
    ): Instant? = withContext(Dispatchers.IO) {
        try {
            val roomsRes = RetrofitClient.apiService.getAllRoom("Bearer $token", homeId)
            if (!roomsRes.isSuccessful) return@withContext null
            val rooms = roomsRes.body()?.rooms.orEmpty()
            if (rooms.isEmpty()) return@withContext null

            val utcFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneId.of("UTC"))

            coroutineScope {
                rooms.map { room ->
                    async {
                        runCatching {
                            val res = RetrofitClient.apiService.getFalls(
                                token = "Bearer $token",
                                roomId = room.id,
                                startTime = utcFmt.format(start),
                                endTime   = utcFmt.format(end)
                            )
                            if (!res.isSuccessful) return@async null
                            res.body()?.alerts.orEmpty()
                                .mapNotNull { a -> runCatching { Instant.parse(a.detectedAt) }.getOrNull() }
                                .filter { it in start..end }
                                .maxOrNull()
                        }.getOrNull()
                    }
                }.awaitAll().filterNotNull().maxOrNull()
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadLastFallInstantAcrossRooms error", e); null
        }
    }

    // 오늘 활동 감시
    fun startActivityWatcher(token: String) {
        activityWatcherJob?.cancel()
        activityWatcherJob = viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val utcFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"))

            while (isActive) {
                val currentRooms = _rooms.value
                if (currentRooms.isEmpty()) {
                    _todayActivityCurrentByRoomId.value = emptyMap()
                    _dangerActivityTodayByRoomId.value = emptyMap()
                    _todayLatestActivityScore.value = 0
                    delay(60_000); continue
                }

                val now = Instant.now()
                val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()
                val latestMap = mutableMapOf<String, Int>()
                val dangerMap = mutableMapOf<String, Boolean>()
                val latestDetail = mutableMapOf<String, Pair<Instant, Int>>()

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
                            val latest = scores.maxByOrNull {
                                runCatching { Instant.parse(it.endTime ?: it.startTime) }
                                    .getOrElse { Instant.EPOCH }
                            }

                            val lastInstant = runCatching {
                                Instant.parse(latest?.endTime ?: latest?.startTime)
                            }.getOrElse { Instant.EPOCH }

                            val lastVal = (latest?.activityScore ?: 0.0).toDouble()
                            val iv = lastVal.toInt().coerceAtLeast(0)

                            latestMap[room.id] = iv
                            dangerMap[room.id] = lastVal >= ACTIVITY_THRESHOLD

                            if (lastInstant != Instant.EPOCH) {
                                latestDetail[room.id] = lastInstant to iv
                            }
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

                runCatching {
                    if (latestDetail.isEmpty()) {
                        _todayLatestActivityScore.value = 0
                    } else {
                        val maxInstant = latestDetail.values.maxByOrNull { it.first }?.first
                        if (maxInstant == null) {
                            _todayLatestActivityScore.value = 0
                        } else {
                            val latestDate = maxInstant.atZone(zone).toLocalDate()
                            val candidates = latestDetail.values.filter { pair ->
                                pair.first.atZone(zone).toLocalDate() == latestDate
                            }
                            val maxScore = candidates.maxOfOrNull { it.second } ?: 0
                            _todayLatestActivityScore.value = maxScore
                        }
                    }
                }.onFailure {
                    _todayLatestActivityScore.value = 0
                }

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
                            } catch (_: Exception) { emptyList() }
                        }
                    }.awaitAll().flatten()
                }

                if (allValues.isEmpty()) 0 else allValues.average().roundToInt().coerceAtLeast(0)
            } catch (e: Exception) {
                Log.e(TAG, "getActivityAvgForDateAcrossHome error", e)
                0
            }
        }
    }

    fun startRespNowWatcher(token: String, roomIds: List<String>) {
        respNowWatcherJob?.cancel()
        if (token.isBlank() || roomIds.isEmpty()) {
            _respNowBpm.value = 0
            return
        }

        respNowWatcherJob = viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val utcFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneId.of("UTC"))

            while (isActive) {
                val now = Instant.now()
                val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()

                val maxNow = withContext(Dispatchers.IO) {
                    try {
                        coroutineScope {
                            roomIds.map { rid ->
                                async {
                                    runCatching {
                                        val res = RetrofitClient.apiService.getRespiration(
                                            token = "Bearer $token",
                                            roomId = rid,
                                            startTime = utcFmt.format(todayStart),
                                            endTime   = utcFmt.format(now)
                                        )
                                        if (!res.isSuccessful) return@async 0

                                        val list = res.body()?.breathing.orEmpty()
                                        val coverNowRates = list.mapNotNull { b ->
                                            val s = parseInstantFlexible(b.startTime, zone)
                                            val e = parseInstantFlexible(b.endTime.ifBlank { b.startTime }, zone)
                                            if (s != null && e != null && !now.isBefore(s) && !now.isAfter(e)) {
                                                b.breathingRate.toInt().coerceAtLeast(0)
                                            } else null
                                        }
                                        coverNowRates.maxOrNull() ?: 0
                                    }.getOrDefault(0)
                                }
                            }.awaitAll().maxOrNull() ?: 0
                        }
                    } catch (_: Exception) { 0 }
                }

                _respNowBpm.value = maxNow

                val ms = System.currentTimeMillis()
                val nextMin = ((ms / 60_000) + 1) * 60_000
                delay(nextMin - ms)
            }
        }
    }

    suspend fun getRespAvgForDateAcrossHome(token: String, homeId: String, date: LocalDate): Int {
        return withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant()
            val end   = date.plusDays(1).atStartOfDay(zone).toInstant()
            val utcFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneId.of("UTC"))

            try {
                val roomsRes = RetrofitClient.apiService.getAllRoom("Bearer $token", homeId)
                if (!roomsRes.isSuccessful) return@withContext 0
                val roomIds = roomsRes.body()?.rooms?.map { it.id }.orEmpty()
                if (roomIds.isEmpty()) return@withContext 0

                val allRates = coroutineScope {
                    roomIds.map { rid ->
                        async {
                            runCatching {
                                val res = RetrofitClient.apiService.getRespiration(
                                    token = "Bearer $token",
                                    roomId = rid,
                                    startTime = utcFmt.format(start),
                                    endTime   = utcFmt.format(end)
                                )
                                if (!res.isSuccessful) return@async emptyList<Double>()
                                res.body()?.breathing
                                    ?.map { it.breathingRate.toDouble() }
                                    ?.filter { it > 0.0 }
                                    .orEmpty()
                            }.getOrDefault(emptyList())
                        }
                    }.awaitAll().flatten()
                }

                if (allRates.isEmpty()) 0 else allRates.average().roundToInt()
            } catch (e: Exception) {
                Log.e(TAG, "getRespAvgForDateAcrossHome error", e)
                0
            }
        }
    }

    private fun parseInstantFlexible(ts: String?, zone: ZoneId): Instant? {
        if (ts.isNullOrBlank()) return null
        runCatching { return Instant.parse(ts) }
        return runCatching {
            val ldt = LocalDateTime.parse(ts, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ldt.atZone(zone).toInstant()
        }.getOrNull()
    }

    fun stopRespNowWatcher() {
        respNowWatcherJob?.cancel()
        respNowWatcherJob = null
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
                        parsedLocalDate == selectedDate || (lp.summaryDate?.startsWith(startDateStr) == true)
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
            val token = _token.value
            if (token.isBlank() || homeId.isBlank()) {
                _entryTotalEnter.value = 0
                _entryTotalExit.value = 0
                return@launch
            }

            try {
                val zone = ZoneId.systemDefault()
                val start = date.atStartOfDay(zone).toInstant()
                val end = date.plusDays(1).atStartOfDay(zone).toInstant()
                val utcFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .withZone(ZoneId.of("UTC"))

                val roomsRes = RetrofitClient.apiService.getAllRoom("Bearer $token", homeId)
                if (!roomsRes.isSuccessful) {
                    _entryTotalEnter.value = 0
                    _entryTotalExit.value = 0
                    return@launch
                }

                val roomIds = roomsRes.body()?.rooms?.map { it.id }.orEmpty()
                if (roomIds.isEmpty()) {
                    _entryTotalEnter.value = 0
                    _entryTotalExit.value = 0
                    return@launch
                }

                val (enterSum, exitSum) = withContext(Dispatchers.IO) {
                    coroutineScope {
                        roomIds.map { rid ->
                            async {
                                runCatching {
                                    val res = RetrofitClient.apiService.getEntryPatterns(
                                        token = "Bearer $token",
                                        roomId = rid,
                                        startTime = utcFmt.format(start),
                                        endTime   = utcFmt.format(end)
                                    )
                                    if (!res.isSuccessful) return@async 0 to 0

                                    var enters = 0
                                    var exits = 0
                                    res.body()?.presences.orEmpty().forEach { p ->
                                        val ts = runCatching { Instant.parse(p.startTime) }.getOrNull()
                                        if (ts != null && ts >= start && ts < end) {
                                            if (p.isPresent) enters++ else exits++
                                        }
                                    }
                                    enters to exits
                                }.getOrDefault(0 to 0)
                            }
                        }.awaitAll().fold(0 to 0) { acc, pair ->
                            (acc.first + pair.first) to (acc.second + pair.second)
                        }
                    }
                }

                _entryTotalEnter.value = enterSum
                _entryTotalExit.value = exitSum
            } catch (e: Exception) {
                Log.e(TAG, "fetchEntryPatternsData error", e)
                _entryTotalEnter.value = 0
                _entryTotalExit.value = 0
            }
        }
    }

    fun stopWatcher() {
        lastFallWatchJob?.cancel(); lastFallWatchJob = null
        activityWatcherJob?.cancel(); activityWatcherJob = null
        stopRespNowWatcher()
    }

    override fun onCleared() {
        AppController.prefs.unregisterOnChangeListener(prefListener)
        stopWatcher()
        super.onCleared()
    }
}
