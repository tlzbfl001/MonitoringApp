package com.aitronbiz.arron.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferenceUtil(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    fun setUserPrefs(int: Int) {
        prefs.edit() { putInt("user", int) }
    }

    fun setStartActivityPrefs(data: String) {
        prefs.edit() { putString("activity", data) }
    }

    fun getUserPrefs(): Int {
        return prefs.getInt("user", 0)
    }

    fun removeAllPrefs() {
        prefs.edit() { clear() }
    }
}