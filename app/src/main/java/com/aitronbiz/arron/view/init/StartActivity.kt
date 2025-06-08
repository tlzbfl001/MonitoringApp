package com.aitronbiz.arron.view.init

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.MainActivity
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DataManager

class StartActivity : AppCompatActivity() {
    private lateinit var dataManager: DataManager
    private lateinit var splashScreen: SplashScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SplashScreen 설치
        splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            ObjectAnimator.ofPropertyValuesHolder(splashScreenView.iconView).apply {
                duration = 3000L
                doOnEnd {
                    splashScreenView.remove()
                }
                start()
            }
        }

        // UI 설정
        setContentView(R.layout.activity_start)
        this.window?.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.WHITE
            navigationBarColor = Color.WHITE
        }

        dataManager = DataManager.getInstance(this)

        lifecycleScope.launchWhenStarted {
            val getUser = dataManager.getUser(AppController.prefs.getUID()) // 사용자 정보 확인
            val isLoggedIn = AppController.prefs.getUID() > 0 && !getUser.email.isNullOrEmpty()

            // 로그인 상태에 따른 화면 전환
            val intent = if (isLoggedIn) {
                Intent(this@StartActivity, MainActivity::class.java)
            } else {
                Intent(this@StartActivity, LoginActivity::class.java)
            }
            startActivity(intent)
            finish()
        }
    }
}