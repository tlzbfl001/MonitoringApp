package com.aitronbiz.arron

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aitronbiz.arron.databinding.ActivityMainBinding
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.view.home.DeviceFragment
import com.aitronbiz.arron.view.home.MainFragment
import com.aitronbiz.arron.view.home.ReportFragment
import com.aitronbiz.arron.view.home.SettingsFragment

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.BLACK

            when(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> setStatusBarIconColor(true)
                else -> setStatusBarIconColor(false)
            }
        }

        replaceFragment1(supportFragmentManager, MainFragment())

        binding.navigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigationHome -> replaceFragment1(supportFragmentManager, MainFragment())
                R.id.navigationDevice -> replaceFragment1(supportFragmentManager, DeviceFragment())
                R.id.navigationReport -> replaceFragment1(supportFragmentManager, ReportFragment())
                R.id.navigationMenu -> replaceFragment1(supportFragmentManager, SettingsFragment())
            }
            true
        }
    }

    private fun setStatusBarIconColor(isDarkText: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                if (isDarkText) {
                    // ✅ 상태바 아이콘을 검정색으로 (밝은 배경에 적합)
                    it.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    // ✅ 상태바 아이콘을 흰색으로 (어두운 배경에 적합)
                    it.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                }
            }
        } else {
            // API 30 이하 처리 (옵션)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (isDarkText) {
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
        }
    }
}