package com.aitronbiz.arron.util

import android.content.Context
import android.util.Log
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.util.CustomUtil.TAG

object TokenManager {
    suspend fun checkAndRefreshJwtToken(context: Context, onSessionExpired: suspend () -> Unit): Boolean {
        val dataManager = DataManager.getInstance(context)
        val jwtToken = AppController.prefs.getToken()
        if (jwtToken.isNullOrBlank()) {
            onSessionExpired() // JWT 자체가 없으면 세션 만료 처리
            return false
        }

        try {
            val getUser = dataManager.getUser(AppController.prefs.getUID())
            val sessionResp = RetrofitClient.authApiService.checkSession("Bearer ${getUser.sessionToken}")
            if (!sessionResp.isSuccessful) {
                onSessionExpired() // 세션 토큰이 유효하지 않으면 세션 만료 처리
                return false
            }

            val jwtResp = RetrofitClient.authApiService.getToken("Bearer ${getUser.sessionToken}")
            if (jwtResp.isSuccessful) {
                val newJwtToken = jwtResp.body()?.token
                if (!newJwtToken.isNullOrBlank()) {
                    Log.d(TAG, "newJwtToken: $newJwtToken")
                    AppController.prefs.saveToken(newJwtToken) // 새 JWT 저장
                    return true
                }
            }
        }catch(e: Exception) {
            Log.e(TAG, "토큰 갱신 실패: ${e.message}")
        }

        return false
    }
}