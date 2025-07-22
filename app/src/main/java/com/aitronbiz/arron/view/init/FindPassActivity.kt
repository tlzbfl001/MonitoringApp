package com.aitronbiz.arron.view.init

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.FindPasswordDTO
import com.aitronbiz.arron.api.dto.SignUpDTO
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.databinding.ActivityFindPassBinding
import com.aitronbiz.arron.databinding.ActivityLoginBinding
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.entity.User
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.log

class FindPassActivity : AppCompatActivity() {
    private var _binding: ActivityFindPassBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityFindPassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 상태바 설정
        this.window?.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.BLACK

            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight = if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
            binding.mainLayout.setPadding(0, statusBarHeight, 0, 0)
        }

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.btnSend.setOnClickListener {
            when {
                binding.etEmail.text.toString().isEmpty() -> Toast.makeText(this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show()
                else -> {
                    lifecycleScope.launch {
                        try {
                            val dto = FindPasswordDTO(
                                email = binding.etEmail.text.toString().trim()
                            )

                            val response = RetrofitClient.authApiService.findPassword(dto)
                            if(response.isSuccessful) {
                                Log.d(TAG, "response: ${response.body()}")
                                Toast.makeText(this@FindPassActivity, "비밀번호 재설정 이메일이 발송되었습니다", Toast.LENGTH_SHORT).show()
                            }else {
                                val errorBody = response.errorBody()?.string()
                                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                                Log.e(TAG, "errorResponse: $errorResponse")
                                Toast.makeText(this@FindPassActivity, errorResponse.code, Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "$e")
                        }
                    }
                }
            }
        }
    }
}