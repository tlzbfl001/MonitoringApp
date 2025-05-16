package kr.aitron.aitron.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferenceUtil(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    fun setUserPrefs(key: String, int: Int) {
        prefs.edit() { putInt(key, int) }
    }

    fun getUserPrefs(): Int {
        return prefs.getInt("prefs", 0)
    }

    fun removeAllPrefs() {
        prefs.edit() { clear() }
    }
}