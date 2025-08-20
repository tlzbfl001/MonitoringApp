package com.aitronbiz.arron.screen.init

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.ResetPasswordDTO
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.databinding.ActivityResetPassBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.hideKeyboard
import com.google.gson.Gson
import kotlinx.coroutines.launch

class ResetPassActivity : AppCompatActivity() {
    private var _binding: ActivityResetPassBinding? = null
    private val binding get() = _binding!!

    private var isPasswordVisible1 = false
    private var isPasswordVisible2 = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityResetPassBinding.inflate(layoutInflater)
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

        val email = intent.getStringExtra("email")
        val otp = intent.getStringExtra("otp")

        if(email == "" || otp == "") {
            val intent = Intent(this, FindPassActivity::class.java)
            startActivity(intent)
        }

        binding.mainLayout.setOnClickListener {
            hideKeyboard(this, it)
        }

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, FindPassActivity::class.java)
            startActivity(intent)
        }

        binding.etPassword.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = binding.etPassword.compoundDrawables[2]
                if (drawableEnd != null) {
                    val extraClickArea = 40 // 여유 영역(px)

                    val drawableWidth = drawableEnd.bounds.width()
                    val rightEdge = binding.etPassword.right
                    val leftEdge = rightEdge - drawableWidth - extraClickArea

                    if (event.rawX >= leftEdge) {
                        isPasswordVisible1 = !isPasswordVisible1
                        if (isPasswordVisible1) {
                            binding.etPassword.inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                            binding.etPassword.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_lock, 0, R.drawable.ic_eye_invisible, 0
                            )
                        } else {
                            binding.etPassword.inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            binding.etPassword.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_lock, 0, R.drawable.ic_eye_visible, 0
                            )
                        }
                        binding.etPassword.setSelection(binding.etPassword.text.length)
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        binding.etConfirmPw.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = binding.etConfirmPw.compoundDrawables[2]
                if (drawableEnd != null) {
                    val extraClickArea = 40 // 여유 영역(px)

                    val drawableWidth = drawableEnd.bounds.width()
                    val rightEdge = binding.etConfirmPw.right
                    val leftEdge = rightEdge - drawableWidth - extraClickArea

                    if (event.rawX >= leftEdge) {
                        isPasswordVisible2 = !isPasswordVisible2
                        if (isPasswordVisible2) {
                            binding.etConfirmPw.inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                            binding.etConfirmPw.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_lock, 0, R.drawable.ic_eye_invisible, 0
                            )
                        } else {
                            binding.etConfirmPw.inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            binding.etConfirmPw.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_lock, 0, R.drawable.ic_eye_visible, 0
                            )
                        }
                        binding.etConfirmPw.setSelection(binding.etConfirmPw.text.length)
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        binding.btnConfirm.setOnClickListener {
            when {
                binding.etPassword.text.toString().isEmpty() -> Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                binding.etPassword.text.toString().length < 8 -> Toast.makeText(this, "비밀번호는 8자 이상 입력해야됩니다.", Toast.LENGTH_SHORT).show()
                binding.etPassword.text.toString().trim() != binding.etConfirmPw.text.toString().trim() -> Toast.makeText(this, "비밀번호가 틀립니다.", Toast.LENGTH_SHORT).show()
                else -> {
                    lifecycleScope.launch {
                        try {
                            val dto = ResetPasswordDTO(
                                email = email!!,
                                otp = otp!!,
                                password = binding.etConfirmPw.text.toString().trim()
                            )

                            val response = RetrofitClient.authApiService.resetPassword(dto)
                            if(response.isSuccessful) {
                                Log.d(TAG, "resetPassword: ${response.body()}")
                                Toast.makeText(this@ResetPassActivity, "비밀번호 재설정이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@ResetPassActivity, LoginActivity::class.java)
                                startActivity(intent)
                            }else {
                                val errorBody = response.errorBody()?.string()
                                if (!errorBody.isNullOrBlank()) {
                                    try {
                                        val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                                        val errorMessage = errorResponse?.code
                                        when (errorMessage) {
                                            "USER_NOT_FOUND" -> {
                                                Toast.makeText(this@ResetPassActivity, "에러가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                                val intent = Intent(this@ResetPassActivity, FindPassActivity::class.java)
                                                startActivity(intent)
                                            }
                                            "INVALID_OTP" -> Toast.makeText(this@ResetPassActivity, "인증번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                                            "OTP_EXPIRED" -> Toast.makeText(this@ResetPassActivity, "인증번호가 만료되었습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        // JSON 파싱 실패 시
                                        Log.e(TAG, "Error parsing error body: ${e.message}")
                                        Toast.makeText(this@ResetPassActivity, "서버 응답 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this@ResetPassActivity, FindPassActivity::class.java)
                                        startActivity(intent)
                                    }
                                } else {
                                    Toast.makeText(this@ResetPassActivity, "에러가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@ResetPassActivity, FindPassActivity::class.java)
                                    startActivity(intent)
                                }
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