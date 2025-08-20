package com.aitronbiz.arron.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Home
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.util.ActivityAlertStore
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.TokenManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    private var activityAlertJob: Job? = null
    private var watcherStarted = false
    private val ACTIVITY_THRESHOLD = 80.0

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

    fun updateSelectedDate(date: LocalDate) { _selectedDate.value = date }

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

    fun startActivityAlertWatcher(token: String) {
        if (watcherStarted) return
        watcherStarted = true

        activityAlertJob?.cancel()
        activityAlertJob = viewModelScope.launch {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneId.of("UTC"))

            while (isActive) {
                val now = Instant.now()
                val start = now.minus(30, ChronoUnit.MINUTES)
                val end = now

                val currentRooms = _rooms.value
                currentRooms.forEach { room ->
                    try {
                        val res = RetrofitClient.apiService.getActivity(
                            token = "Bearer $token",
                            roomId = room.id,
                            startTime = formatter.format(start),
                            endTime   = formatter.format(end)
                        )
                        if (res.isSuccessful) {
                            val scores = res.body()?.activityScores.orEmpty()
                            val lastVal = scores
                                .maxByOrNull { Instant.parse(it.endTime ?: it.startTime) }
                                ?.activityScore?.toDouble()
                                ?: 0.0

                            ActivityAlertStore.setLatestActivity(room.id, lastVal.toInt())
                            val danger = lastVal >= ACTIVITY_THRESHOLD
                            ActivityAlertStore.set(room.id, danger)
                        } else {
                            ActivityAlertStore.setLatestActivity(room.id, 0)
                            ActivityAlertStore.set(room.id, false)
                        }
                    } catch (e: Exception) {
                        ActivityAlertStore.setLatestActivity(room.id, 0)
                        ActivityAlertStore.set(room.id, false)
                    }
                }

                delay(60_000L)
            }
        }
    }

    fun stopActivityAlertWatcher() {
        activityAlertJob?.cancel()
        activityAlertJob = null
        watcherStarted = false
    }
}
