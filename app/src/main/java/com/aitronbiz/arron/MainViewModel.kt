package com.aitronbiz.arron

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.aitronbiz.arron.database.DBHelper.Companion.ACTIVITY
import com.aitronbiz.arron.database.DBHelper.Companion.DAILY_DATA
import com.aitronbiz.arron.database.DBHelper.Companion.LIGHT
import com.aitronbiz.arron.database.DBHelper.Companion.TEMPERATURE
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.entity.Activity
import com.aitronbiz.arron.entity.DailyData
import com.aitronbiz.arron.entity.Light
import com.aitronbiz.arron.entity.Temperature
import com.aitronbiz.arron.util.CustomUtil.getFormattedDate
import com.prolificinteractive.materialcalendarview.CalendarDay

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dataManager = DataManager.getInstance(application)

    private val _dailyActivityUpdated: MutableLiveData<Boolean> = MutableLiveData()
    val dailyActivityUpdated: LiveData<Boolean> = _dailyActivityUpdated

    fun sendDailyData(data: CalendarDay, subjectId: Int, deviceId: Int) {
        val formattedDate = getFormattedDate(data)

        val getData = dataManager.getDailyActivity(deviceId, formattedDate)
        if(getData.isEmpty()) {
            val activityVal = List(24) { (0..100).random() }
            val temperatureVal = List(24) { (0..50).random() }
            val lightVal = List(24) { (0..1000).random() }

            val dates = listOf<String>(
                "${formattedDate}T00:44:30.327959", "${formattedDate}T01:44:30.327959", "${formattedDate}T02:44:30.327959", "${formattedDate}T03:44:30.327959",
                "${formattedDate}T04:44:30.327959", "${formattedDate}T05:44:30.327959", "${formattedDate}T06:44:30.327959", "${formattedDate}T07:44:30.327959",
                "${formattedDate}T08:44:30.327959", "${formattedDate}T09:44:30.327959", "${formattedDate}T10:44:30.327959", "${formattedDate}T11:44:30.327959",
                "${formattedDate}T12:44:30.327959", "${formattedDate}T13:44:30.327959", "${formattedDate}T14:44:30.327959", "${formattedDate}T15:44:30.327959",
                "${formattedDate}T16:44:30.327959", "${formattedDate}T17:44:30.327959", "${formattedDate}T18:44:30.327959", "${formattedDate}T19:44:30.327959",
                "${formattedDate}T20:44:30.327959", "${formattedDate}T21:44:30.327959", "${formattedDate}T22:44:30.327959", "${formattedDate}T23:44:30.327959",
            )

            for(i in activityVal.indices) {
                dataManager.insertActivity(
                    Activity(uid = AppController.prefs.getUID(), subjectId = subjectId, deviceId = deviceId,
                        activity = activityVal[i], createdAt = dates[i])
                )
            }

            for(i in temperatureVal.indices) {
                dataManager.insertTemperature(
                    Temperature(uid = AppController.prefs.getUID(), subjectId = subjectId, deviceId = deviceId,
                        temperature = temperatureVal[i], createdAt = dates[i])
                )
            }

            for(i in lightVal.indices) {
                dataManager.insertLight(
                    Light(uid = AppController.prefs.getUID(), subjectId = subjectId, deviceId = deviceId,
                        light = lightVal[i], createdAt = dates[i])
                )
            }

            var total = 0
            for(i in activityVal.indices) total += activityVal[i]
            val pct = (total * 100) / (activityVal.size * 100)
            dataManager.insertDailyData(
                DailyData(uid = AppController.prefs.getUID(), subjectId = subjectId, deviceId = deviceId,
                activityRate = pct, createdAt = formattedDate)
            )
        }else {
            dataManager.deleteData(ACTIVITY, formattedDate)
            dataManager.deleteData(TEMPERATURE, formattedDate)
            dataManager.deleteData(LIGHT, formattedDate)
            dataManager.deleteData(DAILY_DATA, formattedDate)
        }
    }
}