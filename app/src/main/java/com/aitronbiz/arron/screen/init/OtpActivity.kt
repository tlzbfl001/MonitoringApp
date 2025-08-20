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
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.CheckOtpDTO
import com.aitronbiz.arron.api.dto.FindPasswordDTO
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.databinding.ActivityOtpBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.hideKeyboard
import com.google.gson.Gson
import kotlinx.coroutines.launch

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
                        try {
                            val dto = CheckOtpDTO(
                                email = email,
                                otp = otpCode,
                                type = "forget-password"
                            )
                            Log.d(TAG, "dto: $dto")

                            val response = RetrofitClient.authApiService.checkOtp(dto)
                            if (response.isSuccessful) {
                                val intent = Intent(this@OtpActivity, ResetPassActivity::class.java).apply {
                                    putExtra("otp", otpCode)
                                    putExtra("email", email)
                                }
                                startActivity(intent)
                            } else {
                                val errorBody = response.errorBody()?.string()
                                if (!errorBody.isNullOrBlank()) {
                                    try {
                                        val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                                        val errorMessage = errorResponse?.code
                                        when (errorMessage) {
                                            "USER_NOT_FOUND" -> Toast.makeText(this@OtpActivity, "등록되지 않은 이메일입니다.", Toast.LENGTH_SHORT).show()
                                            "OTP_EXPIRED" -> Toast.makeText(this@OtpActivity, "인증번호가 만료되었습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) { // JSON 파싱 실패 시
                                        Log.e(TAG, "Error parsing error body: $e")
                                        Toast.makeText(this@OtpActivity, "서버 응답 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(this@OtpActivity, "에러가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }catch (e: Exception) {
                            Log.e(TAG, "Exception: $e")
                            Toast.makeText(this@OtpActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}