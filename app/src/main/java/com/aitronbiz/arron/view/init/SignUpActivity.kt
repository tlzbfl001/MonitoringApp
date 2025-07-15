package com.aitronbiz.arron.view.init

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.SignUpDTO
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.ActivitySignUpBinding
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.entity.User
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class SignUpActivity : AppCompatActivity() {
    private var _binding: ActivitySignUpBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 상태바 관련 설정
        this.window?.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.BLACK

            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight = if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
            binding.mainLayout.setPadding(0, statusBarHeight, 0, 0)
        }

        dataManager = DataManager.getInstance(this)

        binding.tvLogin.text = SpannableString("로그인").apply {
            setSpan(UnderlineSpan(), 0, length, 0)
        }

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.btnRegister.setOnClickListener {
            when {
                binding.etName.text.toString().isEmpty() -> Toast.makeText(this, "이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                binding.etEmail.text.toString().isEmpty() -> Toast.makeText(this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show()
                binding.etPassword.text.toString().isEmpty() -> Toast.makeText(this, "비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                else -> {
                    lifecycleScope.launch {
                        try {
                            val dto = SignUpDTO(
                                name = binding.etName.text.toString().trim(),
                                email = binding.etEmail.text.toString().trim(),
                                password = binding.etPassword.text.toString().trim()
                            )

                            val response = RetrofitClient.authApiService.signUpEmail(dto)
                            if(response.isSuccessful) {
                                val signUpResponse = response.body()!!
                                val getToken = RetrofitClient.authApiService.getToken("Bearer ${signUpResponse.sessionToken}")

                                if(getToken.isSuccessful) {
                                    val tokenResponse = getToken.body()!!
                                    val user = User(type= EnumData.EMAIL.name, sessionToken=signUpResponse.sessionToken, username=binding.etName.text.toString().trim(),
                                        email=binding.etEmail.text.toString().trim(), createdAt=LocalDateTime.now().toString())
                                    val checkUser = dataManager.getUserId(EnumData.EMAIL.name, binding.etEmail.text.toString().trim()) // 사용자가 DB에 존재하는지 확인

                                    // 사용자 데이터 저장 or 수정
                                    val insertUser = if (checkUser == 0) dataManager.insertUser(user) else dataManager.updateSocialLoginUser(user)
                                    if(insertUser == false) {
                                        Toast.makeText(this@SignUpActivity, "회원가입에 실패하였습니다", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    val getUserId = dataManager.getUserId(user.type, user.email)
                                    if(getUserId > 0) {
                                        AppController.prefs.saveUID(getUserId) // 사용자 ID preference에 저장
                                        AppController.prefs.saveToken(tokenResponse.token) // 토큰 preference에 저장
                                        Toast.makeText(this@SignUpActivity, "회원가입을 완료했습니다", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
                                        startActivity(intent)
                                    } else {
                                        Toast.makeText(this@SignUpActivity, "회원가입 실패", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Log.e(TAG, "tokenResponse: $getToken")
                                }
                            } else {
                                Log.e(TAG, "response: $response")
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