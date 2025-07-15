package com.aitronbiz.arron

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.databinding.ActivityMainBinding
import com.aitronbiz.arron.service.FirebaseMessagingService
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.TokenManager
import com.aitronbiz.arron.view.home.MainFragment
import com.aitronbiz.arron.view.init.LoginActivity
import com.aitronbiz.arron.view.report.HealthFragment
import com.aitronbiz.arron.view.setting.SettingsFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.BLACK
            setStatusBarIconColor(false)
        }

        replaceFragment1(supportFragmentManager, MainFragment())

        binding.navigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigationHome -> replaceFragment1(supportFragmentManager, MainFragment())
                R.id.navigationMenu -> replaceFragment1(supportFragmentManager, SettingsFragment())
            }
            true
        }

        // JWT 토큰 갱신시 호출
        viewModel.startTokenRefresh {
            lifecycleScope.launch(Dispatchers.Main) {
                AppController.prefs.removeToken()
                Toast.makeText(
                    this@MainActivity,
                    "로그인 세션이 만료되었습니다. 다시 로그인해 주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }

        // FCM 토큰 새로 발급되거나 갱신시 호출
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val fcmToken = task.result
                AppController.prefs.saveFcmToken(fcmToken)

                // JWT 토큰이 준비됐으면 보내기
                val jwtToken = AppController.prefs.getToken()
                if (!jwtToken.isNullOrEmpty()) {
                    FirebaseMessagingService.sendTokenToServer(fcmToken)
                } else {
                    Log.d(TAG, "아직 JWT 없음, 로그인 후에 서버에 FCM 토큰 보내야 함")
                }
            } else {
                Log.e(TAG, "Fetching FCM registration token failed", task.exception)
            }
        }
    }

    private fun setStatusBarIconColor(isDarkText: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                if (isDarkText) {
                    it.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    it.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                }
            }
        } else {
            // API 30 이하 처리
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (isDarkText) {
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopTokenAutoRefresh()
    }
}