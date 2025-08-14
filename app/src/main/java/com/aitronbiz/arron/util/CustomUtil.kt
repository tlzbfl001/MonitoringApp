package com.aitronbiz.arron.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.aitronbiz.arron.AppController
import org.json.JSONObject
import java.util.UUID

object CustomUtil {
    const val TAG = "logTAG2"

    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(nw) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }

    fun hideKeyboard(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun generateRandomUUID(): String {
        return UUID.randomUUID().toString()
    }

    fun getUserInfo(): Pair<String, String> {
        val token = AppController.prefs.getToken() ?: return "" to ""
        return runCatching {
            val parts = token.split(".")
            require(parts.size >= 2) { "Invalid JWT" }
            val payload = parts[1]
            val decoded = Base64.decode(
                payload.padEnd(payload.length + ((4 - payload.length % 4) % 4), '='),
                Base64.URL_SAFE
            )
            val json = JSONObject(String(decoded))
            val name = json.optString("name", "")
            val email = json.optString("email", "")
            name to email
        }.getOrDefault("" to "")
    }
}