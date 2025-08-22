package com.aitronbiz.arron.screen.init

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.aitronbiz.arron.databinding.ActivityTerms3Binding

class Terms3Activity : AppCompatActivity() {
    private var _binding: ActivityTerms3Binding? = null
    private val binding get() = _binding!!

    // SignUpActivity에서만 복원되도록 사용하는 임시 저장소
    private val prefs by lazy { getSharedPreferences("signup_temp", MODE_PRIVATE) }

    private var entryType: Int = 0 // 1: 약관목록 등에서 진입, 2: 회원가입 화면에서 진입

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityTerms3Binding.inflate(layoutInflater)
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

        entryType = intent.getIntExtra("type", 0)

        binding.btnBack.setOnClickListener { goBack() }

        onBackPressedDispatcher.addCallback(this) { goBack() }

        val termsText = """
본 동의서는 서비스 제공을 위해 아래와 같은 개인정보를 수집·이용하는 데 동의하시는 내용을 확인합니다.

1. 수집하는 개인정보 항목
• 아이디
• 이메일
• 주소
• 휴대전화 번호

2. 개인정보 수집·이용 목적
• 회원 관리 및 본인 식별
• 서비스 안내 및 고지사항 전달
• 불만 처리 등 민원 응대
• 마케팅 및 프로모션 정보 제공

3. 보유 및 이용 기간
• 회원 탈퇴 시까지 보유
• 보유 기간 종료 후 해당 정보를 지체 없이 파기

4. 개인정보 제3자 제공
• 원칙적으로 제3자에게 제공하지 않음
• 다만, 법령에 따라 제공이 요구될 경우 예외로 함

5. 정보주체 권리·의무 및 행사 방법
• 개인정보 열람, 정정, 삭제 요구
• 동의 철회 요청
• 권리 행사는 서비스 고객센터 또는 이메일을 통해 가능

6. 동의 거부 권리 및 불이익 안내
• 귀하는 본 동의를 거부할 권리가 있습니다.
• 다만, 동의를 거부할 경우 회원 가입 및 일부 서비스 이용이 제한될 수 있습니다.

[동의 확인]
본인은 상기 내용에 대해 충분히 이해하였으며, 개인정보 수집·이용에 동의합니다.
""".trimIndent()

        binding.tvTermsContent.text = termsText
    }

    private fun goBack() {
        if (entryType == 2) {
            // 회원가입(SignUpActivity)에서 진입한 경우에만 복원 플래그 설정
            prefs.edit().putBoolean("restore_after_terms", true).apply()
            finish()
        } else {
            finish()
        }
    }
}