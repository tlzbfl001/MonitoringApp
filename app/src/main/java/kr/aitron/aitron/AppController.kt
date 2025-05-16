package kr.aitron.aitron

import android.app.Application
import kr.aitron.aitron.util.PreferenceUtil

class AppController : Application() {
    companion object {
        lateinit var prefs: PreferenceUtil
    }

    override fun onCreate() {
        super.onCreate()

        prefs = PreferenceUtil(applicationContext)
    }
}