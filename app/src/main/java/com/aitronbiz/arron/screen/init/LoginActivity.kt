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
import com.aitronbiz.arron.BuildConfig
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.ActivityLoginBinding
import com.aitronbiz.arron.model.EnumData
import com.aitronbiz.arron.model.User
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.api.dto.SignInDTO
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.database.DBHelper.Companion.USER
import com.aitronbiz.arron.util.CustomUtil.hideKeyboard
import com.aitronbiz.arron.screen.MainActivity
import com.aitronbiz.arron.util.CustomUtil.isInternetAvailable
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class LoginActivity : AppCompatActivity() {
    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var googleSignInClient: GoogleSignInClient
    private val GOOGLE_SIGN_IN_REQUEST_CODE = 1000
    private var isPasswordVisible = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityLoginBinding.inflate(layoutInflater)
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

        dataManager = DataManager.getInstance(this)

        AppController.prefs.removeUID() // 이전 UID 제거
        AppController.prefs.removeToken() // 이전 토큰 제거

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.mainLayout.setOnClickListener {
            hideKeyboard(this, it)
        }

        binding.etPassword.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = binding.etPassword.compoundDrawables[2]
                if (drawableEnd != null) {
                    // 클릭 감지 여유 범위(px)
                    val extraClickArea = 40

                    val drawableWidth = drawableEnd.bounds.width()
                    val rightEdge = binding.etPassword.right
                    val leftEdge = rightEdge - drawableWidth - extraClickArea

                    if (event.rawX >= leftEdge) {
                        // 아이콘 클릭 감지
                        isPasswordVisible = !isPasswordVisible

                        if (isPasswordVisible) {
                            // 비밀번호 보이기
                            binding.etPassword.inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                            binding.etPassword.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_lock, 0, R.drawable.ic_eye_invisible, 0
                            )
                        } else {
                            // 비밀번호 숨기기
                            binding.etPassword.inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            binding.etPassword.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_lock, 0, R.drawable.ic_eye_visible, 0
                            )
                        }
                        // 커서 위치 유지
                        binding.etPassword.setSelection(binding.etPassword.text.length)
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        binding.btnLogin.setOnClickListener {
            when {
                binding.etEmail.text.toString().isEmpty() -> Toast.makeText(this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show()
                binding.etPassword.text.toString().isEmpty() -> Toast.makeText(this, "비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                else -> {
                    lifecycleScope.launch {
                        try {
                            val dto = SignInDTO(
                                email = binding.etEmail.text.toString().trim(),
                                password = binding.etPassword.text.toString().trim()
                            )

                            val response = RetrofitClient.authApiService.signInEmail(dto)
                            if(response.isSuccessful) {
                                val loginResponse = response.body()!!
                                val getToken = RetrofitClient.authApiService.getToken("Bearer ${loginResponse.sessionToken}")
                                Log.d(TAG, "signInEmail: ${response.body()}")

                                if(getToken.isSuccessful) {
                                    Log.d(TAG, "getToken: ${getToken.body()}")
                                    val tokenResponse = getToken.body()!!
                                    val user = User(
                                        type= EnumData.EMAIL.name,
                                        sessionToken=loginResponse.sessionToken,
                                        email=binding.etEmail.text.toString().trim(),
                                        createdAt=LocalDateTime.now().toString()
                                    )

                                    val checkUser = dataManager.getUserId(EnumData.EMAIL.name, binding.etEmail.text.toString().trim()) // 사용자가 DB에 존재하는지 확인

                                    val success = if(checkUser == 0) {
                                        dataManager.insertUser(user)
                                    } else {
                                        dataManager.updateData(USER, "sessionToken", user.sessionToken, checkUser)
                                    }

                                    if(!success) {
                                        Toast.makeText(this@LoginActivity, "로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    val getUserId = dataManager.getUserId(user.type, user.email)
                                    if(getUserId > 0) {
                                        AppController.prefs.saveUID(getUserId) // 사용자 ID preference에 저장
                                        AppController.prefs.saveToken(tokenResponse.token) // 토큰 preference에 저장
                                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                        startActivity(intent)
                                    } else {
                                        Toast.makeText(this@LoginActivity, "로그인 실패", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Log.e(TAG, "getToken: ${getToken.code()}")
                                }
                            }else {
                                val errorBody = response.errorBody()?.string()
                                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                                Log.e(TAG, "errorResponse: $errorResponse")
                                if(errorResponse.code == "INVALID_EMAIL") {
                                    Toast.makeText(this@LoginActivity, "이메일 형식이 잘못되었습니다.", Toast.LENGTH_SHORT).show()
                                }else if(errorResponse.code == "INVALID_EMAIL_OR_PASSWORD") {
                                    Toast.makeText(this@LoginActivity, "이메일, 비밀번호가 일치하지않습니다.", Toast.LENGTH_SHORT).show()
                                }else if(errorResponse.message == "Too many requests. Please try again later.") {
                                    Toast.makeText(this@LoginActivity, "요청이 일시적으로 많아 처리에 제한이 있습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "$e")
                        }
                    }
                }
            }
        }

        binding.btnSignIn.setOnClickListener {
            val intent = Intent(this@LoginActivity, SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.btnFindPass.setOnClickListener {
            val intent = Intent(this@LoginActivity, FindPassActivity::class.java)
            startActivity(intent)
        }

        // 구글 로그인
        binding.btnGoogle.setOnClickListener {
            if (isInternetAvailable(this)) {
                signInWithGoogle()
            } else {
                Toast.makeText(this, "네트워크에 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try{
            val account = completedTask.getResult(ApiException::class.java)
            val user = User(
                type = EnumData.GOOGLE.name,
                idToken = account.idToken!!,
                email = account.email!!,
                createdAt = LocalDateTime.now().toString()
            )

            if(user.type != "" && user.idToken != "" && user.email != "" && user.createdAt != "") {
                val intent = Intent(this@LoginActivity, TermsActivity::class.java).apply {
                    putExtra("user", user)
                }
                startActivity(intent)
            }else {
                Toast.makeText(this@LoginActivity, "로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show()
            }
        }catch(e: ApiException) {
            Log.e(TAG, "signInResult:failed code=" + e.statusCode)
        }
    }
}