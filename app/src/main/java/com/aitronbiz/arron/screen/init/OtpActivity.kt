package com.aitronbiz.arron.screen.init

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.CheckOtpDTO
import com.aitronbiz.arron.databinding.ActivityOtpBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.hideKeyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class OtpActivity : AppCompatActivity() {
    private var _binding: ActivityOtpBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.window?.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.BLACK

            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight = if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
            binding.mainLayout.setPadding(0, statusBarHeight, 0, 0)
        }

        val email = intent.getStringExtra("email")

        binding.mainLayout.setOnClickListener {
            hideKeyboard(this, it)
        }

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, FindPassActivity::class.java)
            startActivity(intent)
        }

        val otps = listOf(binding.otp1, binding.otp2, binding.otp3, binding.otp4, binding.otp5, binding.otp6)

        otps.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1) {
                        if (index < otps.size - 1) {
                            otps[index + 1].requestFocus()
                        } else {
                            hideKeyboard(this@OtpActivity, editText)
                        }
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            editText.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (editText.text.isEmpty() && index > 0) {
                        otps[index - 1].requestFocus()
                    }
                }
                false
            }
        }

        binding.btnConfirm.setOnClickListener {
            val otpCode = otps.joinToString("") { it.text.toString() }

            when {
                email == null -> {
                    Toast.makeText(this, "페이지에 오류가 있습니다.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, FindPassActivity::class.java)
                    startActivity(intent)
                }
                otpCode == "" -> Toast.makeText(this, "OTP 코드를 입력하세요.", Toast.LENGTH_SHORT).show()
                else -> {
                    lifecycleScope.launch {
                        // 2초 지나면 프로그래스바 표시
                        val spinnerJob = launch {
                            delay(2_000)
                            binding.progress.isVisible = true
                        }

                        try {
                            val dto = CheckOtpDTO(
                                email = email,
                                otp = otpCode,
                                type = "forget-password"
                            )

                            val response = withTimeoutOrNull(20_000) {
                                withContext(Dispatchers.IO) {
                                    RetrofitClient.authApiService.checkOtp(dto)
                                }
                            }

                            spinnerJob.cancel()
                            binding.progress.isVisible = false

                            if (response == null) {
                                Toast.makeText(this@OtpActivity, "요청이 지연되고 있습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            if (response.isSuccessful) {
                                val intent = Intent(this@OtpActivity, ResetPassActivity::class.java).apply {
                                    putExtra("otp", otpCode)
                                    putExtra("email", email)
                                }
                                startActivity(intent)
                                Toast.makeText(this@OtpActivity, "인증이 성공했습니다.", Toast.LENGTH_SHORT).show()
                            } else {
                                val (code, parseErr) = withContext(Dispatchers.IO) {
                                    try {
                                        val raw = response.errorBody()?.string().orEmpty()
                                        val fast = when {
                                            raw.contains("USER_NOT_FOUND", true) -> "USER_NOT_FOUND"
                                            raw.contains("OTP_EXPIRED", true) -> "OTP_EXPIRED"
                                            else -> null
                                        }
                                        fast to null
                                    } catch (e: Exception) {
                                        null to e
                                    }
                                }

                                when (code) {
                                    "USER_NOT_FOUND" -> Toast.makeText(this@OtpActivity, "등록되지 않은 이메일입니다.", Toast.LENGTH_SHORT).show()
                                    "OTP_EXPIRED" -> Toast.makeText(this@OtpActivity, "인증번호가 만료되었습니다.", Toast.LENGTH_SHORT).show()
                                    else -> {
                                        if (parseErr != null) Log.e(TAG, "Error parsing error body: $parseErr")
                                        Toast.makeText(this@OtpActivity, "서버 응답 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                Log.e(TAG, "Error: $code")
                            }
                        } catch (e: Exception) {
                            spinnerJob.cancel()
                            binding.progress.isVisible = false
                            Log.e(TAG, "Exception: $e")
                            Toast.makeText(this@OtpActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}