package com.aitronbiz.arron.view.init

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.MainActivity

class StartActivity : AppCompatActivity() {
    private lateinit var dataManager: DataManager
    private lateinit var splashScreen: SplashScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        // SplashScreen 설치
        splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            ObjectAnimator.ofPropertyValuesHolder(splashScreenView.iconView).run {
                duration = 3000L
                doOnEnd {
                    splashScreenView.remove()
                }
                start()
            }
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        this.window?.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.WHITE
            navigationBarColor = Color.WHITE
        }

        dataManager = DataManager(this)
        dataManager.open()
        val getUser = dataManager.getUser()
        val isLoggedIn = AppController.prefs.getUserPrefs() > 0 && !getUser.email.isNullOrEmpty()
        dataManager.close()

        // 로그인 상태에 따라 화면 전환
        val intent = if (isLoggedIn) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}