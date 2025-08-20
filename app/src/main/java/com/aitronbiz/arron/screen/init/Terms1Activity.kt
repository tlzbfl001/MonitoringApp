package com.aitronbiz.arron.screen.init

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.aitronbiz.arron.databinding.ActivityTerms1Binding

class Terms1Activity : AppCompatActivity() {
    private var _binding: ActivityTerms1Binding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityTerms1Binding.inflate(layoutInflater)
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

        val type = intent.getIntExtra("type", 0)

        binding.btnBack.setOnClickListener {
            if (type == 1) {
                val intent = Intent(this, TermsActivity::class.java)
                startActivity(intent)
            } else if (type == 2) {
                val intent = Intent(this, SignUpActivity::class.java)
                startActivity(intent)
            }
        }

        val termsText = """
제1조 (목적) 
이 약관은 AITRON(이하 "회사")이 제공하는 ARRON 서비스(이하 "서비스")의 이용과 관련하여 회사와 이용자 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.  

제2조 (용어의 정의) 
• "서비스"란 회사가 제공하는 ARRON 플랫폼을 통한 센서 데이터 시각화 및 IoT 디바이스 관리 서비스를 말합니다. 
• "이용자"란 이 약관에 따라 회사가 제공하는 서비스를 받는 회원 및 비회원을 말합니다. 
• "회원"이란 회사에 개인정보를 제공하여 회원등록을 한 자로서, 회사의 정보를 지속적으로 제공받으며 회사가 제공하는 서비스를 계속적으로 이용할 수 있는 자를 말합니다. 
• "비회원"이란 회원에 가입하지 않고 회사가 제공하는 서비스를 이용하는 자를 말합니다. 

제3조 (약관의 효력 및 변경) 
1. 이 약관은 서비스 화면에 게시하거나 기타의 방법으로 회원에게 공지함으로써 효력이 발생합니다. 
2. 회사는 필요하다고 인정되는 경우 이 약관을 변경할 수 있으며, 변경된 약관은 서비스 화면에 공지하거나 회원에게 통지함으로써 효력이 발생합니다. 
3. 이용자는 변경된 약관에 동의하지 않을 권리가 있으며, 변경된 약관에 동의하지 않는 경우 서비스 이용을 중단하고 회원등록을 해지할 수 있습니다. 

제4조 (서비스의 제공) 
1. 회사는 다음과 같은 서비스를 제공합니다: 
• 센서 데이터 실시간 모니터링 및 시각화 
• IoT 디바이스 통합 관리 및 원격 제어 
• 3D 공간 데이터 시각화 
• 데이터 분석 및 리포팅 기능 
2. 서비스는 연중무휴, 1일 24시간 제공함을 원칙으로 합니다. 단, 시스템 점검이나 기타 정당한 사유가 있는 경우 서비스의 제공을 일시 중단할 수 있습니다. 

제5조 (회원가입 및 회원정보 관리) 
1. 서비스를 이용하고자 하는 자는 회사가 정한 가입 양식에 따라 회원정보를 기입한 후 이 약관에 동의한다는 의사표시를 함으로써 회원가입을 신청합니다. 
2. 회사는 가입신청자의 신청에 대하여 서비스 이용을 승낙함을 원칙으로 합니다. 
3. 회원은 회원가입 시 등록한 사항에 변경이 있는 경우, 상당한 기간 이내에 회사에 대하여 회원정보 수정 등의 방법으로 그 변경사항을 알려야 합니다. 

제6조 (개인정보의 보호) 
회사는 관계 법령이 정하는 바에 따라 회원의 개인정보를 보호하기 위해 노력합니다. 개인정보의 보호 및 사용에 대해서는 관련 법령 및 회사의 개인정보처리방침이 적용됩니다.  

제7조 (회원의 의무) 
1. 회원은 다음 행위를 하여서는 안 됩니다: 
• 신청 또는 변경시 허위 내용의 등록
• 타인의 정보 도용
• 회사가 게시한 정보의 변경
• 회사가 정한 정보 이외의 정보(컴퓨터 프로그램 등) 등의 송신 또는 게시
• 회사 기타 제3자의 저작권 등 지적재산권에 대한 침해
• 회사 기타 제3자의 명예를 손상시키거나 업무를 방해하는 행위
• 외설 또는 폭력적인 메시지, 화상, 음성, 기타 공서양속에 반하는 정보를 서비스에 공개 또는 게시하는 행위 

제8조 (서비스 이용의 제한 및 중단)
1. 회사는 회원이 이 약관의 의무를 위반하거나 서비스의 정상적인 운영을 방해한 경우, 경고, 일시정지, 영구이용정지 등으로 서비스 이용을 단계적으로 제한할 수 있습니다.
2. 회사는 전항에도 불구하고, 주민등록법을 위반한 명의도용 및 결제도용, 전화번호 도용, 저작권법 및 컴퓨터프로그램보호법을 위반한 불법프로그램의 제공 및 운영방해, 정보통신망법을 위반한 불법통신 및 해킹, 악성프로그램의 배포, 접속권한 초과행위 등과 같이 관련법을 위반한 경우에는 즉시 영구이용정지를 할 수 있습니다. 

제9조 
(손해배상) 회사와 회원은 서비스 이용과 관련하여 고의 또는 과실로 상대방에게 손해를 끼친 경우에는 이를 배상할 책임이 있습니다. 다만, 회사는 무료로 제공되는 서비스와 관련하여서는 회원에게 발생한 어떠한 손해에 대해서도 책임을 지지 않습니다.  

제10조 (면책조항)
1. 회사는 천재지변 또는 이에 준하는 불가항력으로 인하여 서비스를 제공할 수 없는 경우에는 서비스 제공에 관한 책임이 면제됩니다.
2. 회사는 회원의 귀책사유로 인한 서비스 이용의 장애에 대하여는 책임을 지지 않습니다.
3. 회사는 회원이 서비스와 관련하여 게재한 정보, 자료, 사실의 신뢰도, 정확성 등의 내용에 관하여는 책임을 지지 않습니다. 

제11조 (준거법 및 관할법원)
1. 이 약관에 명시되지 않은 사항은 관계 법령의 규정에 의합니다.
2. 서비스 이용으로 발생한 분쟁에 대해 소송이 제기되는 경우 회사의 본사 소재지를 관할하는 법원을 관할 법원으로 합니다.
        """.trimIndent()

        binding.tvTermsContent.text = termsText
    }
}