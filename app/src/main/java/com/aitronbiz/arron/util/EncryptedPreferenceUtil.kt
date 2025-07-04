package com.aitronbiz.arron.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import androidx.core.content.edit

class EncryptedPreferenceUtil(context: Context) {
    private val prefs: SharedPreferences

    init {
        // Android Keystore에 저장될 고급 암호화 키를 생성하거나 가져옴
        // AES256-GCM 스펙으로 생성되며, 안전한 키 저장소에 보관됨
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        // 암호화된 SharedPreferences 생성. "secure_prefs"라는 이름의 저장소를 만들며,
        // 키는 AES256-SIV 방식으로 암호화되고, 값은 AES256-GCM 방식으로 암호화됨
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

    fun saveEmail(email: String) {
        prefs.edit() { putString("email", email) }
    }

    fun saveToken(token: String) {
        prefs.edit() { putString("token", token) }
    }

    fun getUID(): Int {
        return prefs.getInt("uid", 0)
    }

    fun getEmail(): String? {
        return prefs.getString("email", null)
    }

    fun getToken(): String? {
        return prefs.getString("token", null)
    }

    fun removeUID() {
        prefs.edit() { remove("uid") }
    }

    fun removeEmail() {
        prefs.edit() { remove("email") }
    }

    fun removeToken() {
        prefs.edit() { remove("token") }
    }
}