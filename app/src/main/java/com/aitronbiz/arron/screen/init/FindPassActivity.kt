package com.aitronbiz.arron.screen.init

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.FindPasswordDTO
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.databinding.ActivityFindPassBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.hideKeyboard
import com.google.gson.Gson
import kotlinx.coroutines.launch

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

        binding.mainLayout.setOnClickListener {
            hideKeyboard(this, it)
        }

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.btnSend.setOnClickListener {
            when {
                binding.etEmail.text.toString().trim().isEmpty() -> Toast.makeText(this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show()
                else -> {
                    lifecycleScope.launch {
                        try {
                            val dto = FindPasswordDTO(
                                email = binding.etEmail.text.toString().trim()
                            )

                            val response = RetrofitClient.authApiService.forgetPassword(dto)
                            if (response.isSuccessful) {
                                Log.d(TAG, "forgetPassword: ${response.body()}")
                                Toast.makeText(this@FindPassActivity, "OTP 코드를 이메일로 보내드렸습니다.", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@FindPassActivity, OtpActivity::class.java).apply {
                                    putExtra("email", binding.etEmail.text.toString().trim())
                                }
                                startActivity(intent)

                            } else {
                                val errorBody = response.errorBody()?.string()
                                if (!errorBody.isNullOrBlank()) {
                                    try {
                                        val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                                        val errorMessage = errorResponse?.code
                                        when (errorMessage) {
                                            "USER_NOT_FOUND" -> Toast.makeText(this@FindPassActivity, "등록되지 않은 이메일입니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        // JSON 파싱 실패 시
                                        Log.e(TAG, "Error parsing error body: $e")
                                        Toast.makeText(this@FindPassActivity, "서버 응답 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(this@FindPassActivity, "에러가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }catch (e: Exception) {
                            Log.e(TAG, "Exception: $e")
                            Toast.makeText(this@FindPassActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}