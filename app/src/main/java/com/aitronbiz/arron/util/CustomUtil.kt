package com.aitronbiz.arron.util

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.aitronbiz.arron.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

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

    fun sendPushNotification(deviceToken: String, title: String, body: String) {
        val url = "https://sendpushnotification-s6qgg22iqq-du.a.run.app"

        val jsonBody = """
        {
          "token": "$deviceToken",
          "title": "$title",
          "body": "$body"
        }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = jsonBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        val client = OkHttpClient()

        // 백그라운드 스레드에서 실행
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d(TAG, "푸시 전송 성공: ${response.body?.string()}")
                } else {
                    Log.e(TAG, "푸시 전송 실패: ${response.code} ${response.body?.string()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}