package com.aitronbiz.arron.view.init

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.navercorp.nid.profile.NidProfileCallback
import com.navercorp.nid.profile.data.NidProfileResponse
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.BuildConfig
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.ActivityLoginBinding
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.entity.User
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.networkStatus
import com.aitronbiz.arron.view.home.MainActivity
import com.google.android.gms.auth.api.Auth
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import java.time.LocalDateTime

class LoginActivity : AppCompatActivity() {
    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataManager = DataManager(this)
        dataManager.open()

        this.window?.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.BLACK
            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight = if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else { 0 }
            binding.mainLayout.setPadding(0, statusBarHeight, 0, 0)
        }

        AppController.prefs.removeAllPrefs()

        // 구글 로그인
        binding.btnGoogle.setOnClickListener {
            if(networkStatus(this)) {
                googleLogin()
            }else {
                Toast.makeText(this, "네트워크에 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 네이버 로그인
        binding.btnNaver.setOnClickListener {
            if(networkStatus(this)) {
                naverLogin()
            }else {
                Toast.makeText(this, "네트워크에 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 카카오 로그인
        binding.btnKakao.setOnClickListener {
            if(networkStatus(this)) {
                kakaoLogin()
            }else {
                Toast.makeText(this, "네트워크에 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        }

//      Log.i(TAG, "${Utility.getKeyHash(this)}")
    }

    private fun googleLogin() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        val gsc = GoogleSignIn.getClient(this, gso)

        val signInIntent = gsc.signInIntent
        startActivityForResult(signInIntent, 1000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 1000) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data!!)
            if(result!!.isSuccess) {
                val acct = result.signInAccount!!
                val user = User(
                    type = EnumData.GOOGLE.name,
                    idToken = acct.idToken!!,
                    accessToken = "",
                    username = "",
                    email = acct.email!!,
                    createdAt = LocalDateTime.now().toString()
                )

                createUser(user)
            }
        }
    }

    private fun createUser(user: User) {
        var getUserId = dataManager.getUserId(user.type, user.email)

        // 사용자 정보 저장
        if(getUserId == 0) dataManager.insertUser(user) else dataManager.updateUser(user)

        getUserId = dataManager.getUserId(user.type, user.email)

        if(getUserId > 0) {
            AppController.prefs.setUserPrefs(getUserId) // 사용자ID 저장
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            startActivity(intent)
        }else {
            Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun naverLogin() {
        val oAuthLoginCallback = object : OAuthLoginCallback {
            override fun onSuccess() {
                NidOAuthLogin().callProfileApi(object : NidProfileCallback<NidProfileResponse> {
                    override fun onSuccess(result: NidProfileResponse) {

                    }

                    override fun onError(errorCode: Int, message: String) {
                        Log.e(TAG, "naverLogin err1: $message")
                    }

                    override fun onFailure(httpStatus: Int, message: String) {
                        Log.e(TAG, "naverLogin err2: $message")
                    }
                })
            }

            override fun onError(errorCode: Int, message: String) {
                Log.e(TAG, "naverLogin err3: $message")
            }

            override fun onFailure(httpStatus: Int, message: String) {
                Log.e(TAG, "naverLogin err4: $message")
            }
        }

        // SDK 객체 초기화
        NaverIdLoginSDK.initialize(this, "", "", getString(R.string.app_name))
        NaverIdLoginSDK.authenticate(this, oAuthLoginCallback)
    }

    private fun kakaoLogin() {
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if(error != null) Log.e(TAG, "kakaoLogin err1: $error") else if (token != null) createKakaoUser(token)
        }

        // 카카오톡이 설치되어있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
        if(UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                if(error != null) {
                    Log.e(TAG, "kakaoLogin err2: $error")
                    if(error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        return@loginWithKakaoTalk
                    }else {
                        UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                    }
                }else if(token != null) {
                    createKakaoUser(token)
                }
            }
        }else {
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }
    }

    private fun createKakaoUser(token: OAuthToken) {
        UserApiClient.instance.me { user, error ->
            if(error == null) {

            }else {
                Log.e(TAG, "UserApiClient: $error")
            }
        }
    }
}