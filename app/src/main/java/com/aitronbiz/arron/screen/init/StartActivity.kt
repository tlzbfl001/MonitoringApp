package com.aitronbiz.arron.screen.init

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import android.view.animation.AnimationUtils
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.util.CustomUtil.isInternetAvailable
import com.aitronbiz.arron.screen.MainActivity
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

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        window.navigationBarColor = Color.BLACK

        if (!isInternetAvailable(this)) {
            window.decorView.post {
                AlertDialog.Builder(this)
                    .setTitle("인터넷 연결 오류")
                    .setMessage("인터넷이 연결되어 있지 않아 앱을 실행할 수 없습니다.")
                    .setCancelable(false)
                    .setPositiveButton("앱 종료") { _, _ -> finishAffinity() }
                    .show()
            }
            return
        }

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

            val getUser = dataManager.getUser(AppController.prefs.getUID())
            val isLoggedIn = AppController.prefs.getUID() > 0 && getUser.email.isNotEmpty()

            if(isLoggedIn) {
                startActivity(Intent(this@StartActivity, MainActivity::class.java))
            } else {
                startActivity(Intent(this@StartActivity, LoginActivity::class.java))
            }
            finish()
        }
    }
}