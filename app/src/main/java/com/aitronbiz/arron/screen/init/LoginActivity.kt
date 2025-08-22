package com.aitronbiz.arron.screen.init

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.BuildConfig
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.DeviceDTO2
import com.aitronbiz.arron.api.dto.HomeDTO1
import com.aitronbiz.arron.api.dto.RoomDTO
import com.aitronbiz.arron.api.dto.SignInDTO
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.database.DBHelper.Companion.USER
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.ActivityLoginBinding
import com.aitronbiz.arron.model.EnumData
import com.aitronbiz.arron.model.User
import com.aitronbiz.arron.screen.MainActivity
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.createData
import com.aitronbiz.arron.util.CustomUtil.hideKeyboard
import com.aitronbiz.arron.util.CustomUtil.isInternetAvailable
import com.aitronbiz.arron.util.CustomUtil.userInfo
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime

class LoginActivity : AppCompatActivity() {
    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var googleSignInClient: GoogleSignInClient
    private val GOOGLE_SIGN_IN_REQUEST_CODE = 1000

    private var isPasswordVisible = false

    private var loadingDialog: Dialog? = null
    private var loadingTextView: TextView? = null

    private fun Int.dp(): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            resources.displayMetrics
        ).toInt()

    private fun showLoading(message: String = "로그인 중...") {
        loadingDialog?.let { dlg ->
            loadingTextView?.text = message
            if (!dlg.isShowing) dlg.show()
            return
        }

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0f)
            setGravity(Gravity.CENTER)
            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            foregroundGravity = Gravity.CENTER
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.TRANSPARENT)
        }

        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleLarge).apply {
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(36.dp(), 36.dp())
        }

        loadingTextView = TextView(this).apply {
            text = message
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTextColor(Color.WHITE)
            alpha = 0.95f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8.dp()
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }

        content.addView(progress)
        content.addView(loadingTextView)
        root.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )

        dialog.setContentView(root)
        dialog.show()

        loadingDialog = dialog
    }

    private fun hideLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
        loadingTextView = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.BLACK

            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight =
                if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
            binding.mainLayout.setPadding(0, statusBarHeight, 0, 0)
        }

        dataManager = DataManager.getInstance(this)

        AppController.prefs.removeUID()
        AppController.prefs.removeToken()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.mainLayout.setOnClickListener { hideKeyboard(this, it) }

        binding.etPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = binding.etPassword.compoundDrawables[2]
                if (drawableEnd != null) {
                    val extraClickArea = 40
                    val drawableWidth = drawableEnd.bounds.width()
                    val rightEdge = binding.etPassword.right
                    val leftEdge = rightEdge - drawableWidth - extraClickArea
                    if (event.rawX >= leftEdge) {
                        isPasswordVisible = !isPasswordVisible
                        if (isPasswordVisible) {
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

        binding.btnLogin.setOnClickListener {
            when {
                binding.etEmail.text.toString().isEmpty() ->
                    Toast.makeText(this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show()
                binding.etPassword.text.toString().isEmpty() ->
                    Toast.makeText(this, "비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                else -> {
                    binding.btnLogin.isEnabled = false
                    lifecycleScope.launch {
                        val spinnerJob = launch {
                            delay(2000)
                            showLoading("로그인 중...")
                        }

                        val finishedInTime = withTimeoutOrNull(10_000) {
                            try {
                                val dto = SignInDTO(
                                    email = binding.etEmail.text.toString().trim(),
                                    password = binding.etPassword.text.toString().trim()
                                )

                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.authApiService.signInEmail(dto)
                                }

                                if (response.isSuccessful) {
                                    val loginResponse = response.body()!!
                                    val getToken = withContext(Dispatchers.IO) {
                                        RetrofitClient.authApiService.getToken("Bearer ${loginResponse.sessionToken}")
                                    }
                                    Log.d(TAG, "signInEmail: ${response.body()}")

                                    if (getToken.isSuccessful) {
                                        Log.d(TAG, "getToken: ${getToken.body()}")
                                        val tokenResponse = getToken.body()!!
                                        val user = User(
                                            type = EnumData.EMAIL.name,
                                            sessionToken = loginResponse.sessionToken,
                                            email = binding.etEmail.text.toString().trim(),
                                            createdAt = LocalDateTime.now().toString()
                                        )

                                        val checkUser = withContext(Dispatchers.IO) {
                                            dataManager.getUserId(EnumData.EMAIL.name, user.email)
                                        }

                                        val success = withContext(Dispatchers.IO) {
                                            if (checkUser == 0) dataManager.insertUser(user)
                                            else dataManager.updateData(
                                                USER,
                                                "sessionToken",
                                                user.sessionToken,
                                                checkUser
                                            )
                                        }

                                        if (!success) {
                                            Toast.makeText(this@LoginActivity, "로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                                            return@withTimeoutOrNull
                                        }

                                        val getUserId = withContext(Dispatchers.IO) {
                                            dataManager.getUserId(user.type, user.email)
                                        }

                                        if (getUserId > 0) {
                                            AppController.prefs.saveUID(getUserId)
                                            AppController.prefs.saveToken(tokenResponse.token)

                                            if(createData()) {
                                                withContext(Dispatchers.Main) {
                                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                                }
                                            }else {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(this@LoginActivity, "로그인 실패", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "getToken: $getToken")
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(this@LoginActivity, "로그인 실패", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    val errorResponse = try {
                                        Gson().fromJson(errorBody, ErrorResponse::class.java)
                                    } catch (_: Exception) { null }

                                    Log.e(TAG, "errorResponse: $errorResponse")

                                    when {
                                        errorResponse?.code == "INVALID_EMAIL" ->
                                            Toast.makeText(this@LoginActivity,"이메일 형식이 잘못되었습니다.", Toast.LENGTH_SHORT).show()
                                        errorResponse?.code == "INVALID_EMAIL_OR_PASSWORD" ->
                                            Toast.makeText(this@LoginActivity, "이메일, 비밀번호가 일치하지않습니다.", Toast.LENGTH_SHORT).show()
                                        errorResponse?.message == "Too many requests. Please try again later." ->
                                            Toast.makeText(this@LoginActivity, "로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                                        else -> Toast.makeText(this@LoginActivity, "로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "$e")
                                Toast.makeText(this@LoginActivity, "로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                            }
                        } != null

                        spinnerJob.cancelAndJoin()
                        hideLoading()
                        binding.btnLogin.isEnabled = true

                        if (!finishedInTime) {
                            Toast.makeText(this@LoginActivity, "로그인에 실패하였습니다. 다시시도해주세요.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        binding.btnSignIn.setOnClickListener {
            startActivity(Intent(this@LoginActivity, SignUpActivity::class.java))
        }

        binding.btnFindPass.setOnClickListener {
            startActivity(Intent(this@LoginActivity, FindPassActivity::class.java))
        }

        binding.btnGoogle.setOnClickListener {
            if (isInternetAvailable(this)) {
                binding.btnGoogle.isEnabled = false
                lifecycleScope.launch {
                    val spinnerJob = launch {
                        delay(2000)
                        showLoading("로그인 중...")
                    }

                    val finishedInTime = withTimeoutOrNull(10_000) {
                        try {
                            signInWithGoogle()
                        } catch (e: Exception) {
                            Log.e(TAG, "Google SignIn error: $e")
                            Toast.makeText(
                                this@LoginActivity,
                                "구글 로그인에 실패하였습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } != null

                    spinnerJob.cancelAndJoin()
                    hideLoading()
                    binding.btnGoogle.isEnabled = true

                    if (!finishedInTime) {
                        Toast.makeText(
                            this@LoginActivity,
                            "구글 로그인에 실패하였습니다. 다시 시도해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(this, "네트워크에 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE)
        }
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
        try {
            val account = completedTask.getResult(ApiException::class.java)
            userInfo = User(
                type = EnumData.GOOGLE.value,
                idToken = account.idToken ?: "",
                email = account.email ?: "",
                createdAt = LocalDateTime.now().toString()
            )

            if (
                userInfo.type.isNotEmpty() &&
                userInfo.idToken.isNotEmpty() &&
                userInfo.email.isNotEmpty() &&
                userInfo.createdAt!!.isNotEmpty()
            ) {
                val intent = Intent(this@LoginActivity, TermsActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this@LoginActivity, "로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.e(TAG, "signInResult:failed code=${e.statusCode}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideLoading()
        _binding = null
    }
}
