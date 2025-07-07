package com.aitronbiz.arron

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.aitronbiz.arron.databinding.ActivityMainBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.view.home.MainFragment
import com.aitronbiz.arron.view.report.HealthFragment
import com.aitronbiz.arron.view.setting.SettingsFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
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

            when(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> setStatusBarIconColor(true)
                else -> setStatusBarIconColor(false)
            }
        }

        replaceFragment1(supportFragmentManager, MainFragment())

        binding.navigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigationHome -> replaceFragment1(supportFragmentManager, MainFragment())
                R.id.navigationReport -> replaceFragment1(supportFragmentManager, HealthFragment())
                R.id.navigationMenu -> replaceFragment1(supportFragmentManager, SettingsFragment())
            }
            true
        }

//        viewModel.startTokenRefresh {
//            lifecycleScope.launch(Dispatchers.Main) {
//                AppController.prefs.removeToken()
//                Toast.makeText(
//                    this@MainActivity,
//                    "로그인 세션이 만료되었습니다. 다시 로그인해 주세요.",
//                    Toast.LENGTH_SHORT
//                ).show()
//                val intent = Intent(this@MainActivity, LoginActivity::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                startActivity(intent)
//            }
//        }

        // FCM 토큰 가져오기 → Firestore에 저장
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                saveTokenToFireStore(token)
                AppController.prefs.saveFcmToken(token)
            } else {
                Log.e(TAG, "토큰 가져오기 실패", task.exception)
            }
        }
    }

    private fun saveTokenToFireStore(token: String) {
        val firestore = FirebaseFirestore.getInstance()
        val userId = if(AppController.prefs.getEmail() == null) {
            AppController.prefs.getUID().toString()
        } else AppController.prefs.getEmail()

        val data = hashMapOf(
            "token" to token,
            "updatedAt" to System.currentTimeMillis(),
            "updatedAtStr" to LocalDateTime.now().toString()
        )

        if(userId != null) {
            firestore.collection("fcmTokens")
                .document(userId)
                .set(data)
                .addOnSuccessListener {
                    Log.d(TAG, "새 토큰 Firestore 저장 성공")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "새 토큰 Firestore 저장 실패", e)
                }
        }else {
            Log.e(TAG, "새 토큰 Firestore 저장 실패")
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