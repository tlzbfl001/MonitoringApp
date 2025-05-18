package com.aitronbiz.aitron

import android.app.Application
import com.aitronbiz.aitron.util.PreferenceUtil

class AppController : Application() {
    companion object {
        lateinit var prefs: PreferenceUtil
    }

    override fun onCreate() {
        super.onCreate()

        prefs = PreferenceUtil(applicationContext)
    }
}