package com.aitronbiz.arron.util

import android.util.Log
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object ActivityAlertStore {
    private val _alertByRoom = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val alertByRoom: StateFlow<Map<String, Boolean>> = _alertByRoom
    private val jobsByRoom = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(Dispatchers.Default)

    // 자동해제 없이 상태만 변경
    fun set(roomId: String, alert: Boolean) {
        _alertByRoom.update { old ->
            val current = old[roomId]
            if (current == alert) old else old + (roomId to alert)
        }
        // false로 바꿀 땐 기존 타이머도 정리
        if (!alert) {
            jobsByRoom.remove(roomId)?.cancel()
        }
        Log.d(TAG, "alert: $alert")
    }

    // 임계치 초과 시 호출: 켜고 durationMs 뒤 자동으로 끄기 (연속 트리거 시 타이머 리셋)
    fun trigger(roomId: String, durationMs: Long = 60_000L) {
        set(roomId, true)

        // 기존 타이머 취소 후 재시작
        jobsByRoom.remove(roomId)?.cancel()
        jobsByRoom[roomId] = scope.launch {
            delay(durationMs)
            set(roomId, false)
        }
    }

    fun clearAll() {
        _alertByRoom.value = emptyMap()
        jobsByRoom.values.forEach { it.cancel() }
        jobsByRoom.clear()
    }

    fun clear(roomId: String) {
        set(roomId, false)
    }

    fun shutdown() {
        clearAll()
        scope.cancel()
    }
}