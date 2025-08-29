package com.aitronbiz.arron.screen

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.databinding.ActivityMainBinding
import com.aitronbiz.arron.screen.device.DeviceFragment
import com.aitronbiz.arron.screen.home.MainFragment
import com.aitronbiz.arron.screen.setting.SettingsFragment
import com.aitronbiz.arron.screen.init.LoginActivity
import com.aitronbiz.arron.service.FirebaseMessagingService
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.viewmodel.MainViewModel
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        if (savedInstanceState == null) {
            replaceFragment2(supportFragmentManager, MainFragment(), null)
            binding.navigation.selectedItemId = R.id.home
        }

        binding.navigation.setOnItemSelectedListener { item ->
            val current = supportFragmentManager.findFragmentById(R.id.mainFrame)
            when (item.itemId) {
                R.id.home -> {
                    if (current is MainFragment) return@setOnItemSelectedListener true
                    replaceFragment2(supportFragmentManager, MainFragment(), null)
                    true
                }
                R.id.device -> {
                    if (current is DeviceFragment) return@setOnItemSelectedListener true
                    replaceFragment2(supportFragmentManager, DeviceFragment(), null)
                    true
                }
                R.id.settings -> {
                    if (current is SettingsFragment) return@setOnItemSelectedListener true
                    replaceFragment2(supportFragmentManager, SettingsFragment(), null)
                    true
                }
                else -> false
            }
        }

        // JWT 토큰 만료 처리
        viewModel.startTokenRefresh {
            lifecycleScope.launch(Dispatchers.Main) {
                AppController.prefs.removeToken()
                android.widget.Toast.makeText(
                    this@MainActivity,
                    "로그인 세션이 만료되었습니다. 다시 로그인해 주세요.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }

        // FCM 토큰 처리
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val fcmToken = task.result
                AppController.prefs.saveFcmToken(fcmToken)

                val jwtToken = AppController.prefs.getToken()
                if (!jwtToken.isNullOrEmpty()) {
                    FirebaseMessagingService.sendTokenToServer(fcmToken)
                } else {
                    Log.d(TAG, "아직 JWT 없음, 로그인 후에 서버에 FCM 토큰 보내야 함")
                }
            } else {
                Log.e(TAG, "FCM error", task.exception)
            }
        }
    }

    private fun setStatusBarIconColor(isDarkText: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                if (isDarkText)
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                else
                    0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        (if (isDarkText) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopTokenAutoRefresh()
    }
}