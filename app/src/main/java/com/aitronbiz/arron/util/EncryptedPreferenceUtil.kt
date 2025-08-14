package com.aitronbiz.arron.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import androidx.core.content.edit

class EncryptedPreferenceUtil(context: Context) {
    private val prefs: SharedPreferences

    init {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        prefs = EncryptedSharedPreferences.create(
            "secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveUID(uid: Int) {
        prefs.edit() { putInt("uid", uid) }
    }

    fun saveToken(token: String) {
        prefs.edit() { putString("token", token) }
    }

    fun saveFcmToken(token: String) {
        prefs.edit() { putString("fcm_token", token) }
    }

    fun getUID(): Int {
        return prefs.getInt("uid", 0)
    }

    fun getToken(): String? {
        return prefs.getString("token", null)
    }

    fun getFcmToken(): String? {
        return prefs.getString("fcm_token", null)
    }

    fun removeUID() {
        prefs.edit() { remove("uid") }
    }

    fun removeToken() {
        prefs.edit() { remove("token") }
    }
}