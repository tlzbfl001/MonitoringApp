package com.aitronbiz.arron.util

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.aitronbiz.arron.R
import org.json.JSONObject
import retrofit2.Response
import java.util.UUID

object CustomUtil {
    const val TAG = "logTAG2"
    var location = 1

    fun replaceFragment1(fragmentManager: FragmentManager, fragment: Fragment?) {
        fragmentManager.beginTransaction().apply {
            setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
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

    @JvmStatic
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun setStatusBar(context: Activity, mainLayout: ConstraintLayout) {
        context.window?.apply {
            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight = if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else { 0 }
            mainLayout.setPadding(0, statusBarHeight, 0, 0)
        }
    }

    fun hideKeyboard(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun generateRandomUUID(): String {
        return UUID.randomUUID().toString()
    }

    fun getIdFromJwtToken(jwtToken: String): String? {
        try {
            val parts = jwtToken.split(".")
            if (parts.size != 3) {
                throw IllegalArgumentException("Invalid JWT token format")
            }

            val payload = parts[1]

            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            val payloadJson = String(decodedBytes, charset("UTF-8"))

            val jsonObject = JSONObject(payloadJson)
            return jsonObject.getString("id")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun <T> safeApiCall(
        maxRetries: Int = Int.MAX_VALUE, // 무한 재시도
        delayMillis: Long = 5000,        // 5초 간격 재시도
        apiCall: suspend () -> Response<T>
    ): Response<T>? {
        var attempt = 0
        while (attempt < maxRetries) {
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    return response
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            attempt++
            kotlinx.coroutines.delay(delayMillis)
        }
        return null
    }
}