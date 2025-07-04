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
        val sessionToken = dataManager.getUser(AppController.prefs.getUID()).accessToken
        if (sessionToken.isBlank()) {
            onSessionExpired()
            return false
        }

        try {
            // 세션 유효성 검사
            val sessionResp = RetrofitClient.authApiService.checkSession("Bearer $sessionToken")
            Log.d(TAG, "sessionResp.isSuccessful: ${sessionResp.isSuccessful}")
            if (!sessionResp.isSuccessful) {
                onSessionExpired()
                return false
            }

            // JWT 토큰 갱신
            val jwtResp = RetrofitClient.authApiService.getToken("Bearer $sessionToken")
            if (jwtResp.isSuccessful) {
                val jwtToken = jwtResp.body()?.token
                Log.d(TAG, "RefreshJwtToken: $jwtToken")

                if (!jwtToken.isNullOrBlank()) {
                    AppController.prefs.saveToken(jwtToken)
                    return true
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "토큰 갱신 실패: ${e.message}")
        }

        return false
    }
}