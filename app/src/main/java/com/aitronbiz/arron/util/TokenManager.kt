package com.aitronbiz.arron.util

import android.content.Context
import android.util.Log
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.util.CustomUtil.TAG

object TokenManager {
    suspend fun checkAndRefreshJwtToken(context: Context, onSessionExpired: suspend () -> Unit): Boolean {
        val jwtToken = AppController.prefs.getToken()
        if (jwtToken.isNullOrBlank()) {
            onSessionExpired()
            return false
        }

        try {
            val sessionResp = RetrofitClient.authApiService.checkSession("Bearer $jwtToken")
            if (!sessionResp.isSuccessful) {
                onSessionExpired()
                return false
            }

            val jwtResp = RetrofitClient.authApiService.getToken("Bearer $jwtToken")
            if (jwtResp.isSuccessful) {
                val newJwtToken = jwtResp.body()?.token
                Log.e(TAG, "newJwtToken: $newJwtToken")
                if (!newJwtToken.isNullOrBlank()) {
                    AppController.prefs.saveToken(newJwtToken)
                    return true
                }
            }
        }catch(e: Exception) {
            Log.e(TAG, "토큰 갱신 실패: ${e.message}")
        }

        return false
    }
}