package com.aitronbiz.arron.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.DeviceDTO
import com.aitronbiz.arron.api.dto.HomeDTO
import com.aitronbiz.arron.api.dto.RoomDTO
import com.aitronbiz.arron.model.User
import com.aitronbiz.arron.screen.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

object CustomUtil {
    const val TAG = "logTAG2"
    var userInfo = User()

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

    fun createData(): Boolean {
        return runBlocking {
            val getAllHome = RetrofitClient.apiService.getAllHome("Bearer ${AppController.prefs.getToken()}")
            if (getAllHome.isSuccessful) {
                if(getAllHome.body()?.homes!!.isEmpty()) {
                    var homeId = ""
                    var roomId = ""
                    var deviceId = ""

                    val homeDTO = HomeDTO(
                        name = "나의 홈",
                        province = "test",
                        city = "test",
                        street = "test",
                        detailAddress = "test",
                        postalCode = "12345"
                    )

                    val createHome = RetrofitClient.apiService.createHome("Bearer ${AppController.prefs.getToken()}", homeDTO)

                    if (createHome.isSuccessful) {
                        Log.d(TAG, "createHome: ${createHome.body()}")
                        homeId = createHome.body()!!.home.id

                        val roomDTO = RoomDTO(name = "나의 장소", homeId = homeId)
                        val createRoom = RetrofitClient.apiService.createRoom("Bearer ${AppController.prefs.getToken()}", roomDTO)
                        if (createRoom.isSuccessful) {
                            Log.d(TAG, "createRoom: ${createRoom.body()}")
                            roomId = createRoom.body()!!.room.id

                            val deviceDTO = DeviceDTO(
                                name = "나의 기기",
                                serialNumber = "test",
                                homeId = homeId,
                                roomId = roomId
                            )
                            val createDevice = RetrofitClient.apiService.createDevice(
                                "Bearer ${AppController.prefs.getToken()}",
                                deviceDTO
                            )
                            if(createDevice.isSuccessful) {
                                Log.d(TAG, "createRoom: ${createDevice.body()}")
                                deviceId = createDevice.body()!!.device.id
                            } else {
                                Log.e(TAG, "createDevice: $createDevice")
                            }

                            if(homeId == "" || roomId == "" || deviceId == "") {
                                RetrofitClient.apiService.deleteHome("Bearer ${AppController.prefs.getToken()}", homeId)
                                RetrofitClient.apiService.deleteRoom("Bearer ${AppController.prefs.getToken()}", roomId)
                                RetrofitClient.apiService.deleteDevice("Bearer ${AppController.prefs.getToken()}", deviceId)
                                false
                            } else {
                                true
                            }
                        } else {
                            Log.e(TAG, "createRoom: $createRoom")
                            false
                        }
                    } else {
                        Log.e(TAG, "createHome: $createHome")
                        false
                    }
                } else {
                    true
                }
            } else {
                Log.e(TAG, "getAllHome: $getAllHome")
                false
            }
        }
    }
}