package com.aitronbiz.arron.util

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

object ActivityAlertStore {
    private val _alertByRoom = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val jobsByRoom = mutableMapOf<String, Job>()
    val latestActivityByRoom = MutableStateFlow<Map<String, Int>>(emptyMap())
    val latestRespirationByRoom = MutableStateFlow<Map<String, Int>>(emptyMap())

    fun set(roomId: String, alert: Boolean) {
        _alertByRoom.update { old ->
            val current = old[roomId]
            if (current == alert) old else old + (roomId to alert)
        }
        if (!alert) {
            jobsByRoom.remove(roomId)?.cancel()
        }
    }

    fun setLatestActivity(roomId: String, score: Int) {
        latestActivityByRoom.value = latestActivityByRoom.value.toMutableMap().apply {
            this[roomId] = score
        }
    }

    fun setLatestResp(roomId: String, bpm: Int) {
        latestRespirationByRoom.value = latestRespirationByRoom.value.toMutableMap().apply {
            this[roomId] = bpm
        }
    }

    fun clearAll() {
        _alertByRoom.value = emptyMap()
        jobsByRoom.values.forEach { it.cancel() }
        jobsByRoom.clear()
    }
}