package com.aitronbiz.arron

import android.app.Application
import com.aitronbiz.arron.util.PreferenceUtil

class AppController : Application() {
    companion object {
        lateinit var prefs: PreferenceUtil
    }

    override fun onCreate() {
        super.onCreate()

//        KakaoSdk.init(this, resources.getString(R.string.kakaoNativeAppKey)) // KaKao SDK 초기화

        prefs = PreferenceUtil(applicationContext)
    }
}