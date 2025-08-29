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
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.SignUpDTO
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.ActivitySignUpBinding
import com.aitronbiz.arron.model.EnumData
import com.aitronbiz.arron.model.User
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class SignUpActivity : AppCompatActivity() {
    private var _binding: ActivitySignUpBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var isPasswordVisible1 = false
    private var isPasswordVisible2 = false
    private var checkAll = true
    private var check1 = true
    private var check2 = true
    private var check3 = true

    private val prefs by lazy { getSharedPreferences("signup_temp", MODE_PRIVATE) }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 상태바 설정
        this.window?.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.BLACK

            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight =
                if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
            binding.mainLayout.setPadding(0, statusBarHeight, 0, 0)
        }

        dataManager = DataManager.getInstance(this)

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // 비밀번호 입력창 토글
        binding.etPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = binding.etPassword.compoundDrawables[2]
                if (drawableEnd != null) {
                    val extraClickArea = 40
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

        // 비밀번호 확인 입력창 토글
        binding.etConfirmPw.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = binding.etConfirmPw.compoundDrawables[2]
                if (drawableEnd != null) {
                    val extraClickArea = 40
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

        // 체크박스 로직
        binding.checkAll.setOnClickListener {
            binding.checkAll.isChecked = checkAll
            checkAll = !checkAll
            if (binding.checkAll.isChecked) {
                binding.check1.isChecked = true
                binding.check2.isChecked = true
                binding.check3.isChecked = true
            } else {
                binding.check1.isChecked = false
                binding.check2.isChecked = false
                binding.check3.isChecked = false
            }
        }
        binding.check1.setOnClickListener {
            binding.check1.isChecked = check1
            check1 = !check1
            updateCheckAllState()
        }
        binding.check2.setOnClickListener {
            binding.check2.isChecked = check2
            check2 = !check2
            updateCheckAllState()
        }
        binding.check3.setOnClickListener {
            binding.check3.isChecked = check3
            check3 = !check3
            updateCheckAllState()
        }

        binding.btnTerms1.setOnClickListener { openTerms(Terms1Activity::class.java) }
        binding.btnTerms2.setOnClickListener { openTerms(Terms2Activity::class.java) }
        binding.btnTerms3.setOnClickListener { openTerms(Terms3Activity::class.java) }

        binding.btnRegister.setOnClickListener {
            when {
                binding.etName.text.toString().isEmpty() ->
                    Toast.makeText(this, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                binding.etEmail.text.toString().isEmpty() ->
                    Toast.makeText(this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                binding.etPassword.text.toString().isEmpty() ->
                    Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                binding.etPassword.text.toString().length < 8 ->
                    Toast.makeText(this, "비밀번호는 8자 이상 입력해야됩니다.", Toast.LENGTH_SHORT).show()
                binding.etPassword.text.toString().trim() !=
                        binding.etConfirmPw.text.toString().trim() ->
                    Toast.makeText(this, "비밀번호가 틀립니다.", Toast.LENGTH_SHORT).show()
                !binding.check1.isChecked || !binding.check2.isChecked || !binding.check3.isChecked ->
                    Toast.makeText(this, "필수 이용약관에 모두 체크해주세요.", Toast.LENGTH_SHORT).show()
                else -> {
                    lifecycleScope.launch {
                        try {
                            val dto = SignUpDTO(
                                name = binding.etName.text.toString().trim(),
                                email = binding.etEmail.text.toString().trim(),
                                password = binding.etPassword.text.toString().trim()
                            )

                            val response = RetrofitClient.authApiService.signUpEmail(dto)
                            if (response.isSuccessful) {
                                val signUpResponse = response.body()!!
                                val getToken = RetrofitClient.authApiService
                                    .getToken("Bearer ${signUpResponse.sessionToken}")

                                if (getToken.isSuccessful) {
                                    val tokenResponse = getToken.body()!!
                                    val user = User(
                                        type = EnumData.EMAIL.name,
                                        sessionToken = signUpResponse.sessionToken,
                                        email = binding.etEmail.text.toString().trim(),
                                        createdAt = LocalDateTime.now().toString()
                                    )
                                    val checkUser = dataManager.getUserId(
                                        EnumData.EMAIL.name,
                                        binding.etEmail.text.toString().trim()
                                    )

                                    // 사용자 데이터 저장 or 수정
                                    val insertUser =
                                        if (checkUser == 0) dataManager.insertUser(user)
                                        else dataManager.updateSocialLoginUser(user)
                                    if (insertUser == false) {
                                        Toast.makeText(
                                            this@SignUpActivity,
                                            "회원가입에 실패하였습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@launch
                                    }

                                    val getUserId = dataManager.getUserId(user.type, user.email)
                                    if (getUserId > 0) {
                                        AppController.prefs.saveUID(getUserId)
                                        AppController.prefs.saveToken(tokenResponse.token)

                                        prefs.edit().clear().apply()

                                        Toast.makeText(
                                            this@SignUpActivity,
                                            "회원가입을 완료했습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        val intent = Intent(
                                            this@SignUpActivity,
                                            LoginActivity::class.java
                                        )
                                        startActivity(intent)
                                    } else {
                                        Toast.makeText(
                                            this@SignUpActivity,
                                            "회원가입 실패",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Log.e(TAG, "getToken: ${getToken.code()}")
                                }
                            } else {
                                val errorBody = response.errorBody()?.string()
                                val errorResponse =
                                    Gson().fromJson(errorBody, ErrorResponse::class.java)
                                Log.e(TAG, "errorResponse: $errorResponse")
                                when {
                                    errorResponse.code == "INVALID_EMAIL" -> {
                                        Toast.makeText(
                                            this@SignUpActivity,
                                            "이메일 형식이 잘못되었습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    errorResponse.code == "USER_ALREADY_EXISTS" -> {
                                        Toast.makeText(
                                            this@SignUpActivity,
                                            "이미 존재하는 이메일입니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    errorResponse.message == "Too many requests. Please try again later." -> {
                                        Toast.makeText(
                                            this@SignUpActivity,
                                            "요청이 일시적으로 많아 처리에 제한이 있습니다. 잠시 후 다시 시도해주세요.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
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

    override fun onResume() {
        super.onResume()
        val shouldRestore = prefs.getBoolean("restore_after_terms", false)
        if (shouldRestore) {
            binding.etName.setText(prefs.getString("name", "") ?: "")
            binding.etEmail.setText(prefs.getString("email", "") ?: "")
            prefs.edit().remove("restore_after_terms").apply()
        }

        binding.etPassword.text?.clear()
        binding.etConfirmPw.text?.clear()
        binding.checkAll.isChecked = false
        binding.check1.isChecked = false
        binding.check2.isChecked = false
        binding.check3.isChecked = false
    }

    private fun openTerms(clazz: Class<*>) {
        prefs.edit()
            .putString("name", binding.etName.text?.toString()?.trim())
            .putString("email", binding.etEmail.text?.toString()?.trim())
            .apply()
        startActivity(Intent(this, clazz).putExtra("type", 2))
    }

    private fun updateCheckAllState() {
        binding.checkAll.isChecked =
            binding.check1.isChecked && binding.check2.isChecked && binding.check3.isChecked
    }
}