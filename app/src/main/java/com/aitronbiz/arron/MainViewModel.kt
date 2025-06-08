package com.aitronbiz.arron

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.database.DBHelper.Companion.ACTIVITY
import com.aitronbiz.arron.database.DBHelper.Companion.DAILY_DATA
import com.aitronbiz.arron.database.DBHelper.Companion.LIGHT
import com.aitronbiz.arron.database.DBHelper.Companion.TEMPERATURE
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.entity.Activity
import com.aitronbiz.arron.entity.DailyData
import com.aitronbiz.arron.entity.Light
import com.aitronbiz.arron.entity.Temperature
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.getFormattedDate
import com.aitronbiz.arron.util.TokenManager
import com.prolificinteractive.materialcalendarview.CalendarDay
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val dataManager = DataManager.getInstance(context)

    private val _dailyActivityUpdated: MutableLiveData<Boolean> = MutableLiveData()
    val dailyActivityUpdated: LiveData<Boolean> = _dailyActivityUpdated

    private var refreshJob: Job? = null

    fun sendDailyData(subjectId: Int, deviceId: Int) {
        val activityVal = (0..100).random()
        val temperatureVal = (0..50).random()
        val lightVal = (0..1000).random()

        dataManager.insertActivity(
            Activity(uid = AppController.prefs.getUID(), subjectId = subjectId, deviceId = deviceId,
                activity = activityVal, createdAt = LocalDateTime.now().toString())
        )

        dataManager.insertTemperature(
            Temperature(uid = AppController.prefs.getUID(), subjectId = subjectId, deviceId = deviceId,
                temperature = temperatureVal, createdAt = LocalDateTime.now().toString())
        )

        dataManager.insertLight(
            Light(uid = AppController.prefs.getUID(), subjectId = subjectId, deviceId = deviceId,
                light = lightVal, createdAt = LocalDateTime.now().toString())
        )

        var total = 0
        val getDailyActivity = dataManager.getDailyActivity(deviceId, LocalDate.now().toString())
        for(i in getDailyActivity.indices) total += getDailyActivity[i].activity
        val pct = (total * 100) / (getDailyActivity.size * 100)

        val getDailyData = dataManager.getDailyData(deviceId, LocalDate.now().toString())
        if(getDailyData == 0) {
            dataManager.insertDailyData(DailyData(uid = AppController.prefs.getUID(), subjectId = subjectId,
                deviceId = deviceId, activityRate = pct, createdAt = LocalDate.now().toString()))
        }else {
            dataManager.updateDailyData(deviceId, pct)
        }
    }

    /*fun sendDailyData(data: CalendarDay, subjectId: Int, deviceId: Int) {
        val formattedDate = getFormattedDate(data)

        val getData = dataManager.getDailyActivity(deviceId, formattedDate)
        if(getData.isEmpty()) {
            var result1 = false
            var result2 = false
            var result3 = false
            val activityVal = List(6) { (0..100).random() }
            val temperatureVal = List(6) { (0..40).random() }
            val lightVal = List(6) { (0..1000).random() }

            val dates = listOf<String>(
                "${formattedDate}T05:44:30.327959", "${formattedDate}T07:44:30.327959", "${formattedDate}T08:44:30.327959",
                "${formattedDate}T10:44:30.327959", "${formattedDate}T11:44:30.327959", "${formattedDate}T18:44:30.327959"
            )

            for(i in activityVal.indices) {
                result1 = dataManager.insertActivity(
                    Activity(uid = AppController.prefs.getUID(), subjectId = subjectId, deviceId = deviceId,
                        activity = activityVal[i], createdAt = dates[i])
                )
            }

            for(i in temperatureVal.indices) {
                result2 = dataManager.insertTemperature(
                    Temperature(uid = AppController.prefs.getUID(), subjectId = subjectId, deviceId = deviceId,
                        temperature = temperatureVal[i], createdAt = dates[i])
                )
            }

            for(i in lightVal.indices) {
                result3 = dataManager.insertLight(
                    Light(uid = AppController.prefs.getUID(), subjectId = subjectId, deviceId = deviceId,
                        light = lightVal[i], createdAt = dates[i])
                )
            }

            Log.d(TAG, "result1: $result1")
            Log.d(TAG, "result2: $result2")
            Log.d(TAG, "result3: $result3")

            if(result1) {
                var total = 0
                for(i in activityVal.indices) total += activityVal[i]
                val pct = (total * 100) / (activityVal.size * 100)
                dataManager.insertDailyData(
                    DailyData(uid = AppController.prefs.getUID(), subjectId = subjectId, deviceId = deviceId,
                        activityRate = pct, createdAt = formattedDate)
                )
            }
        }
    }*/

    fun startTokenRefresh(onSessionExpired: suspend () -> Unit) {
        if (refreshJob?.isActive == true) return

        refreshJob = viewModelScope.launch {
            while (isActive) {
                TokenManager.checkAndRefreshJwtToken(context, onSessionExpired)
                delay(5 * 60 * 1000L) // 5분 대기
            }
        }
    }

    fun stopTokenAutoRefresh() {
        refreshJob?.cancel()
    }
}