package com.aitronbiz.arron.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.api.response.EntryPatternsResponse
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class EntryPatternsViewModel : ViewModel() {
    private val _token = MutableStateFlow(AppController.prefs.getToken().orEmpty())
    val token: StateFlow<String> = _token

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "token") {
            _token.value = AppController.prefs.getToken().orEmpty()
        }
    }

    init {
        AppController.prefs.registerOnChangeListener(prefListener)
    }

    override fun onCleared() {
        AppController.prefs.unregisterOnChangeListener(prefListener)
        super.onCleared()
    }

    private fun bearer(): String = "Bearer ${_token.value}"

    // ───────────── 출입 패턴 (신규 home 단위 API 응답 보관) ─────────────
    private val _entryPatternsSummary = MutableStateFlow<EntryPatternsResponse?>(null)
    val entryPatternsSummary: StateFlow<EntryPatternsResponse?> = _entryPatternsSummary

    private val _entryPatterns = MutableStateFlow<EntryPatternsResponse?>(null)
    val entryPatterns: StateFlow<EntryPatternsResponse?> = _entryPatterns

    // ───────────── 기타 상태 (필요시 사용) ─────────────
    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms

    private val _selectedRoomId = MutableStateFlow("")
    private val _avgActivityAllRooms = MutableStateFlow(0)
    val avgActivityAllRooms: StateFlow<Int> = _avgActivityAllRooms

    private var job: Job? = null
    fun stop() { job?.cancel(); job = null }

    fun resetState(homeId: String, date: LocalDate = LocalDate.now()) {
        _entryPatterns.value = null
        _entryPatternsSummary.value = null
        _rooms.value = emptyList()
        _selectedRoomId.value = ""
        _avgActivityAllRooms.value = 0
        fetchEntryPatternsData(homeId, date)
    }

    fun checkNotifications(onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tk = _token.value
                if (tk.isBlank()) {
                    withContext(Dispatchers.Main) { onResult(false) }
                    return@launch
                }
                val res = RetrofitClient.apiService.getNotification("Bearer $tk", 1, 40)
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

    fun fetchAvgActivityForHome(homeId: String, date: LocalDate) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant()
            val end = if (date == LocalDate.now()) Instant.now()
            else date.plusDays(1).atStartOfDay(zone).toInstant()
            val fmt = DateTimeFormatter.ISO_INSTANT

            try {
                val roomsRes = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getAllRoom(bearer(), homeId)
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
                                token = bearer(),
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
}