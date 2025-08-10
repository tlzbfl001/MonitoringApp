package com.aitronbiz.arron.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Home
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _selectedHomeId = MutableStateFlow("")

    private var refreshJob: Job? = null

    var homes by mutableStateOf<List<Home>>(emptyList())
    var selectedHomeName by mutableStateOf("나의 홈")

    var rooms by mutableStateOf<List<Room>>(emptyList())
    var presenceByRoomId by mutableStateOf<Map<String, Boolean>>(emptyMap())
    private var isAnyPresent by mutableStateOf(false)
    var selectedRoomId by mutableStateOf<String?>(null)

    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    private fun setSelectedHomeId(id: String) {
        _selectedHomeId.value = id
        // 선택 홈이 바뀌면 재실 정보도 갱신
        val token = AppController.prefs.getToken()
        if (!token.isNullOrEmpty() && id.isNotBlank()) {
            refreshPresenceForHome(token, id)
        } else {
            clearPresenceState()
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
    }

    fun fetchHomes(token: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getAllHome("Bearer $token")
                if (response.isSuccessful) {
                    homes = response.body()?.homes ?: emptyList()
                }else {
                    Log.e(TAG, "getAllHome: ${response.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectHome(home: Home) {
        selectedHomeName = home.name
        setSelectedHomeId(home.id)
    }

    // 특정 홈의 devices -> roomId들 수집 후 getPresence를 호출해 상태 갱신
    private fun refreshPresenceForHome(token: String, homeId: String) {
        viewModelScope.launch {
            try {
                // devices 조회해서 roomId 목록 추출
                val devRes = RetrofitClient.apiService.getAllDevice("Bearer $token", homeId)
                val roomIds = devRes.body()?.devices
                    ?.mapNotNull { it.roomId.takeIf { id -> id.isNotBlank() } }
                    ?.distinct()
                    .orEmpty()

                if (roomIds.isEmpty()) {
                    clearPresenceState()
                    return@launch
                }

                // 룸 정보 & 재실 상태 조회
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

                // 상태 반영
                val orderedRooms = roomIds.mapNotNull { roomMap[it] }
                val anyPresent = presenceMap.values.any { it }

                rooms = orderedRooms
                presenceByRoomId = presenceMap
                isAnyPresent = anyPresent

                // 선택 룸 결정
                val firstPresent = presenceMap.entries.firstOrNull { it.value }?.key
                selectedRoomId = firstPresent ?: orderedRooms.firstOrNull()?.id

            } catch (e: Exception) {
                Log.e(TAG, "refreshPresenceForHome error", e)
                clearPresenceState()
            }
        }
    }

    // 룸 선택
    fun selectRoom(roomId: String) {
        selectedRoomId = roomId
    }

    // 재실 상태 초기화
    private fun clearPresenceState() {
        rooms = emptyList()
        presenceByRoomId = emptyMap()
        isAnyPresent = false
        selectedRoomId = null
    }

    // 알림 읽음 여부 확인
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
}