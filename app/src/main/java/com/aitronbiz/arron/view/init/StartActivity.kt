package com.aitronbiz.arron.view.init

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import android.view.animation.AnimationUtils
import androidx.annotation.RequiresApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.MainActivity
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StartActivity : AppCompatActivity() {
    private lateinit var dataManager: DataManager

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        // AppCompat 테마 조기 적용
        setTheme(R.style.Theme_Teltron)

        // SplashScreen 설치(Android 12+)
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        window.statusBarColor = Color.TRANSPARENT

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        window.navigationBarColor = Color.BLACK

        dataManager = DataManager.getInstance(this)

        val loadingImage: ImageView = findViewById(R.id.loadingImage)
        val rotate = AnimationUtils.loadAnimation(this, R.anim.rotate)
        loadingImage.startAnimation(rotate)

        ObjectAnimator.ofFloat(loadingImage, View.ROTATION, 0f, 360f).apply {
            duration = 1200
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }

        lifecycleScope.launch {
            delay(1200L)

            val userId = AppController.prefs.getUID()
            val getUser = dataManager.getUser(userId)
            val isLoggedIn = userId > 0 && getUser.email.isNotEmpty()

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