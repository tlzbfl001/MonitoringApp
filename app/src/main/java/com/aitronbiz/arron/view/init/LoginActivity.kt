package com.aitronbiz.arron.view.init

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.BuildConfig
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.IdTokenDTO
import com.aitronbiz.arron.api.dto.LoginDTO
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.ActivityLoginBinding
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.entity.User
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.MainActivity
import com.aitronbiz.arron.util.CustomUtil.networkStatus
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class LoginActivity : AppCompatActivity() {
    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var googleSignInClient: GoogleSignInClient
    private val GOOGLE_SIGN_IN_REQUEST_CODE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataManager = DataManager.getInstance(this)

        // 상태바 관련 설정
        this.window?.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.BLACK
            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight = if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else { 0 }
            binding.mainLayout.setPadding(0, statusBarHeight, 0, 0)
        }

        AppController.prefs.removeUID()  // 이전 UID 제거

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 구글 로그인
        binding.btnGoogle.setOnClickListener {
            test(EnumData.GOOGLE.name)
//            if (networkStatus(this)) {
//                signInWithGoogle()
//            } else {
//                Toast.makeText(this, "네트워크에 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show()
//            }
        }

        // 네이버 로그인
        binding.btnNaver.setOnClickListener {
            test(EnumData.NAVER.name)
            /*if (networkStatus(this)) {
                val oAuthLoginCallback = object : OAuthLoginCallback {
                    override fun onSuccess() {
                        NidOAuthLogin().callProfileApi(object : NidProfileCallback<NidProfileResponse> {
                            override fun onSuccess(result: NidProfileResponse) {

                            }

                            override fun onError(errorCode: Int, message: String) {
                                Log.e(TAG, message)
                            }

                            override fun onFailure(httpStatus: Int, message: String) {
                                Log.e(TAG, message)
                            }
                        })
                    }

                    override fun onError(errorCode: Int, message: String) {
                        Log.e(TAG, message)
                    }

                    override fun onFailure(httpStatus: Int, message: String) {
                        Log.e(TAG, message)
                    }
                }

                // SDK 객체 초기화
                NaverIdLoginSDK.initialize(this, "", "", getString(R.string.app_name))
                NaverIdLoginSDK.authenticate(this, oAuthLoginCallback)
            } else {
                Toast.makeText(this, "네트워크에 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            }*/
        }

        // 카카오 로그인
        binding.btnKakao.setOnClickListener {
            test(EnumData.KAKAO.name)
            /*if (networkStatus(this)) {
                val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                    if (error != null) Log.e(TAG, "$error") else if (token != null) createKakaoUser(token)
                }

                // 카카오톡이 설치되어있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
                if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                    UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                        if (error != null) {
                            Log.e(TAG, "$error")
                            if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                                return@loginWithKakaoTalk
                            } else {
                                UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                            }
                        } else if (token != null) {
                            createKakaoUser(token)
                        }
                    }
                } else {
                    UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                }
            } else {
                Toast.makeText(this, "네트워크에 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            }*/
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(TAG, "requestCode: $requestCode")
        if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            Log.d(TAG, "task: ${task.isSuccessful}")
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // ✅ 사용자 정보 꺼내기
            val displayName = account.displayName
            val email = account.email
            val photoUrl = account.photoUrl
            val idToken = account.idToken // 서버에 검증 요청할 때 사용

            Log.d(TAG, "Name: $displayName")
            Log.d(TAG, "Email: $email")
            Log.d(TAG, "Photo URL: $photoUrl")
            Log.d(TAG, "ID Token: $idToken")
        } catch (e: ApiException) {
            Log.w("GOOGLE_LOGIN", "signInResult:failed code=" + e.statusCode)
        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == 1000) {
//            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data!!)
//            Log.d(TAG, "result: ${result!!.status}")
//            if (result!!.isSuccess) {
//                val acct = result.signInAccount!!
//                val user = User(type = EnumData.GOOGLE.name, idToken = acct.idToken!!, email = acct.email!!,
//                    createdAt = LocalDateTime.now().toString())
//                createUser(user)
//            }
//        }
//    }

    private fun createKakaoUser(token: OAuthToken) {
        UserApiClient.instance.me { user, error ->
            if (error == null) {

            } else {
                Log.e(TAG, "$error")
            }
        }
    }

    private fun createUser(user: User) {
        lifecycleScope.launch {
            try {
                val loginDTO = LoginDTO(provider = EnumData.GOOGLE.value, idToken = IdTokenDTO(token = user.idToken))
                val response = RetrofitClient.apiService.loginWithGoogle(loginDTO)

                if (response.isSuccessful) {
                    Log.d(TAG, "response: $response")

                    val loginResponse = response.body()!!
                    val getToken = RetrofitClient.apiService.getToken("Bearer ${loginResponse.sessionToken}")

                    if (getToken.isSuccessful) {
                        Log.d(TAG, "getToken: $getToken")

                        val tokenResponse = getToken.body()!!
                        val checkUser = dataManager.getUserId(user.type, user.email) // 사용자가 DB에 존재하는지 확인
                        user.sessionToken = loginResponse.sessionToken // 세션토큰 저장

                        // 사용자 데이터 저장 or 수정
                        val insertUser = if (checkUser == 0) dataManager.insertUser(user) else dataManager.updateUser(user)
                        if (insertUser == false) {
                            Toast.makeText(this@LoginActivity, "로그인에 실패하였습니다", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val getUserId = dataManager.getUserId(user.type, user.email)
                        if (getUserId > 0) {
                            AppController.prefs.saveUID(getUserId) // 사용자 ID preference에 저장
                            AppController.prefs.saveToken(tokenResponse.token) // 토큰 preference에 저장

                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@LoginActivity, "로그인 실패", Toast.LENGTH_SHORT).show()
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

    private fun test(type: String) {
        val user = User(type = type, idToken = "", accessToken = "", username = "",
            email = "test", createdAt = LocalDateTime.now().toString())
        val checkUser = dataManager.getUserId(user.type, user.email)

        val insertUser = if (checkUser == 0) dataManager.insertUser(user) else dataManager.updateUser(user)
        if (insertUser == false) {
            Toast.makeText(this@LoginActivity, "로그인에 실패하였습니다", Toast.LENGTH_SHORT).show()
            return
        }

        val getUserId = dataManager.getUserId(type, user.email)
        if (getUserId > 0) {
            AppController.prefs.saveUID(getUserId)
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            startActivity(intent)
        } else {
            Toast.makeText(this@LoginActivity, "로그인 실패", Toast.LENGTH_SHORT).show()
        }
    }
}