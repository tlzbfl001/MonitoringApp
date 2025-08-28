package com.aitronbiz.arron.screen.setting

import com.aitronbiz.arron.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class Terms3Fragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                Terms3ScreenForFragment(activity = requireActivity())
            }
        }
    }
}

@Composable
private fun Terms3ScreenForFragment(
    activity: FragmentActivity
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 상단 바
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, top = 15.dp, end = 20.dp, bottom = 10.dp),
        ) {
            IconButton(
                onClick = { activity.onBackPressedDispatcher.onBackPressed() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(23.dp)
                )
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "개인정보처리방침",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(13.dp))
        }

        Spacer(Modifier.height(12.dp))

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

        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = termsText,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}