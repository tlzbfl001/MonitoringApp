package com.aitronbiz.arron

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.aitronbiz.arron.databinding.ActivityMainBinding
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.view.device.DeviceFragment
import com.aitronbiz.arron.view.home.MainFragment
import com.aitronbiz.arron.view.report.ReportFragment
import com.aitronbiz.arron.view.setting.SettingsFragment

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

            when(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> setStatusBarIconColor(true)
                else -> setStatusBarIconColor(false)
            }
        }

        replaceFragment1(supportFragmentManager, MainFragment())

        binding.navigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigationHome -> replaceFragment1(supportFragmentManager, MainFragment())
                R.id.navigationReport -> replaceFragment1(supportFragmentManager, ReportFragment())
                R.id.navigationMenu -> replaceFragment1(supportFragmentManager, SettingsFragment())
            }
            true
        }

        /*viewModel.startTokenRefresh {
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    if(AppController.prefs.getToken() == null) {
                        Toast.makeText(this@MainActivity, "로그인 세션이 만료되었습니다. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }else {
                        AppController.prefs.removeToken()
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                }
            }
        }*/
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