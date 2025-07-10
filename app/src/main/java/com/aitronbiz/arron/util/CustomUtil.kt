package com.aitronbiz.arron.util

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Base64
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.aitronbiz.arron.R
import org.json.JSONObject
import java.util.UUID

object CustomUtil {
    const val TAG = "logTAG2"

    fun replaceFragment1(fragmentManager: FragmentManager, fragment: Fragment?) {
        fragmentManager.beginTransaction().apply {
            setCustomAnimations(
                R.anim.slide_in_right, // 진입 애니메이션
                R.anim.slide_out_left, // 퇴장 애니메이션
                R.anim.slide_in_left, // 팝 진입 애니메이션
                R.anim.slide_out_right // 팝 퇴장 애니메이션
            )
            replace(R.id.mainFrame, fragment!!)
            addToBackStack(null)
            commit()
        }
    }

    fun replaceFragment2(fragmentManager: FragmentManager, fragment: Fragment?, bundle: Bundle?) {
        fragmentManager.beginTransaction().apply {
            setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            fragment?.arguments = bundle
            replace(R.id.mainFrame, fragment!!)
            addToBackStack(null)
            commit()
        }
    }

    fun networkStatus(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    fun setStatusBar(context: Activity, mainLayout: ConstraintLayout) {
        context.window?.apply {
            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight = if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else { 0 }
            mainLayout.setPadding(0, statusBarHeight, 0, 0)
        }
    }

    fun generateRandomUUID(): String {
        return UUID.randomUUID().toString()
    }

    fun getIdFromJwtToken(jwtToken: String): String? {
        try {
            // JWT를 . 으로 split해서 payload 추출
            val parts = jwtToken.split(".")
            if (parts.size != 3) {
                throw IllegalArgumentException("Invalid JWT token format")
            }

            val payload = parts[1]

            // Base64 디코딩 (URL_SAFE 옵션)
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            val payloadJson = String(decodedBytes, charset("UTF-8"))

            // JSON 파싱
            val jsonObject = JSONObject(payloadJson)

            // 예: "id" 필드 가져오기
            return jsonObject.getString("id")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}