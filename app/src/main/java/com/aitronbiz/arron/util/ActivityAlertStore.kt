package com.aitronbiz.arron.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

object ActivityAlertStore {
    private val _alertByRoom = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val alertByRoom: StateFlow<Map<String, Boolean>> = _alertByRoom

    fun set(roomId: String, alert: Boolean) {
        _alertByRoom.update { old ->
            val current = old[roomId]
            if (current == alert) old else old + (roomId to alert)
        }
    }
}