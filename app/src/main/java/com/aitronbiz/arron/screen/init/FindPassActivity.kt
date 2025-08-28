package com.aitronbiz.arron.screen.init

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.FindPasswordDTO
import com.aitronbiz.arron.databinding.ActivityFindPassBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.hideKeyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
                binding.etEmail.text.toString().trim().isEmpty() ->
                    Toast.makeText(this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show()

                else -> {
                    lifecycleScope.launch {
                         val spinnerJob = launch {
                             delay(2_000)
                             binding.progress.isVisible = true
                         }

                        try {
                            val email = binding.etEmail.text.toString().trim()
                            val dto = FindPasswordDTO(email = email)

                            val response = withTimeoutOrNull(20_000) {
                                withContext(Dispatchers.IO) {
                                    RetrofitClient.authApiService.forgetPassword(dto)
                                }
                            }

                             spinnerJob.cancel()
                             binding.progress.isVisible = false

                            if (response == null) {
                                Toast.makeText(this@FindPassActivity, "요청이 지연되고 있습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            if (response.isSuccessful) {
                                Toast.makeText(this@FindPassActivity, "OTP 코드를 이메일로 보내드렸습니다.", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@FindPassActivity, OtpActivity::class.java).apply {
                                    putExtra("email", email)
                                }
                                startActivity(intent)
                            } else {
                                val (code, parseErr) = withContext(Dispatchers.IO) {
                                    try {
                                        val raw = response.errorBody()?.string().orEmpty()
                                        val fast = when {
                                            raw.contains("USER_NOT_FOUND", true) -> "USER_NOT_FOUND"
                                            else -> null
                                        }
                                        fast to null
                                    } catch (e: Exception) {
                                        null to e
                                    }
                                }

                                when (code) {
                                    "USER_NOT_FOUND" ->
                                        Toast.makeText(this@FindPassActivity, "등록되지 않은 이메일입니다.", Toast.LENGTH_SHORT).show()
                                    else ->
                                        Toast.makeText(this@FindPassActivity, "서버 응답 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                }
                                Log.e(TAG, "Error: $code")
                            }
                        } catch (e: Exception) {
                             spinnerJob.cancel()
                             binding.progress.isVisible = false

                            Log.e(TAG, "Exception: $e")
                            Toast.makeText(this@FindPassActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}