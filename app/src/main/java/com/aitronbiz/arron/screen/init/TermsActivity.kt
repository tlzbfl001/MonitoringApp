package com.aitronbiz.arron.screen.init

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.IdTokenDTO
import com.aitronbiz.arron.api.dto.LoginDTO
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.ActivityTermsBinding
import com.aitronbiz.arron.model.User
import com.aitronbiz.arron.screen.MainActivity
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.userInfo
import kotlinx.coroutines.launch

class TermsActivity : AppCompatActivity() {
    private var _binding: ActivityTermsBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var checkAll = true
    private var check1 = true
    private var check2 = true
    private var check3 = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityTermsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 상태바 관련 설정
        this.window?.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.BLACK

            val resourceId =
                context.resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight =
                if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
            binding.mainLayout.setPadding(0, statusBarHeight, 0, 0)
        }

        dataManager = DataManager.getInstance(this)

        Log.d(TAG, "userInfo: $userInfo")

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.checkAll.setOnClickListener {
            binding.checkAll.isChecked = checkAll
            checkAll = !checkAll
            if(binding.checkAll.isChecked) {
                binding.check1.isChecked = true
                binding.check2.isChecked = true
                binding.check3.isChecked = true
            }else {
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

        binding.btnTerms1.setOnClickListener {
            val intent = Intent(this, Terms1Activity::class.java)
            intent.putExtra("type", 1)
            startActivity(intent)
        }

        binding.btnTerms2.setOnClickListener {
            val intent = Intent(this, Terms2Activity::class.java)
            intent.putExtra("type", 1)
            startActivity(intent)
        }

        binding.btnTerms3.setOnClickListener {
            val intent = Intent(this, Terms3Activity::class.java)
            intent.putExtra("type", 1)
            startActivity(intent)
        }

        binding.btnConfirm.setOnClickListener {
            if (binding.check1.isChecked && binding.check2.isChecked && binding.check3.isChecked) {
                lifecycleScope.launch {
                    try {
                        val dto = LoginDTO(
                            provider = userInfo.type,
                            idToken = IdTokenDTO(token = userInfo.idToken)
                        )

                        val response = RetrofitClient.authApiService.loginWithGoogle(dto)
                        if(response.isSuccessful) {
                            val res = response.body()!!
                            Log.d(TAG, "loginWithGoogle: $res}")

                            val getToken = RetrofitClient.authApiService.getToken("Bearer ${res.sessionToken}")
                            if(getToken.isSuccessful) {
                                val tokenResponse = getToken.body()!!
                                Log.d(TAG, "getToken: $tokenResponse")

                                userInfo.sessionToken = res.sessionToken // 세션토큰 저장
                                var getUserId = dataManager.getUserId(userInfo.type, userInfo.email) // 사용자가 DB에 존재하는지 확인

                                // 사용자 데이터 저장 or 수정
                                if(getUserId == 0) {
                                    dataManager.insertUser(userInfo)
                                    getUserId = dataManager.getUserId(userInfo.type, userInfo.email)

                                    if(getUserId > 0) {
                                        AppController.prefs.saveUID(getUserId) // 사용자 ID preference에 저장
                                        AppController.prefs.saveToken(tokenResponse.token) // 토큰 preference에 저장
                                    } else {
                                        Toast.makeText(this@TermsActivity, "로그인 실패", Toast.LENGTH_SHORT).show()
                                    }
                                }else {
                                    dataManager.updateSocialLoginUser(userInfo)
                                    AppController.prefs.saveUID(getUserId) // 사용자 ID preference에 저장
                                    AppController.prefs.saveToken(tokenResponse.token) // 토큰 preference에 저장
                                }

                                val getAllHome = RetrofitClient.apiService.getAllHome("Bearer ${tokenResponse.token}")
                                if (getAllHome.isSuccessful) {
                                    if(getAllHome.body()?.homes!!.isEmpty()) {
                                        val intent = Intent(this@TermsActivity, InitAddActivity::class.java)
                                        startActivity(intent)
                                    }else {
                                        val intent = Intent(this@TermsActivity, MainActivity::class.java)
                                        startActivity(intent)
                                    }
                                } else {
                                    Log.e(TAG, "getAllHome: $getAllHome")
                                }
                            } else {
                                Toast.makeText(this@TermsActivity, "로그인 실패", Toast.LENGTH_SHORT).show()
                                Log.e(TAG, "getToken: ${getToken.code()}")
                            }
                        } else {
                            Toast.makeText(this@TermsActivity, "로그인 실패", Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "loginWithGoogle: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@TermsActivity, "로그인 실패", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "$e")
                    }
                }
            }else {
                Toast.makeText(this, "필수 이용약관에 모두 체크해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCheckAllState() {
        if (binding.check1.isChecked && binding.check2.isChecked && binding.check3.isChecked) {
            binding.checkAll.isChecked = true
        }

        if (!binding.check1.isChecked || !binding.check2.isChecked || !binding.check3.isChecked) {
            binding.checkAll.isChecked = false
        }
    }
}