package com.aitronbiz.arron.screen.setting

import com.aitronbiz.arron.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
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
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2

class TermsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { TermsInfoScreenForFragment(requireActivity()) }
        }
    }
}

@Composable
private fun TermsInfoScreenForFragment(
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
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            androidx.compose.material.IconButton(onClick = { activity.onBackPressedDispatcher.onBackPressed() }) {
                androidx.compose.material.Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            androidx.compose.material.Text(
                "서비스 정책",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(Modifier.height(15.dp))

        TermsListItem(
            title = "서비스 약관",
            onClick = {
                val f = Terms1Fragment()
                replaceFragment2(activity.supportFragmentManager, f, null)
            }
        )
        TermsDivider()

        TermsListItem(
            title = "개인정보 수집 및 이용동의",
            onClick = {
                val f = Terms2Fragment()
                replaceFragment2(activity.supportFragmentManager, f, null)
            }
        )
        TermsDivider()

        TermsListItem(
            title = "개인정보 처리방침",
            onClick = {
                val f = Terms3Fragment()
                replaceFragment2(activity.supportFragmentManager, f, null)
            }
        )
        TermsDivider()
    }
}

@Composable
private fun TermsListItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            color = Color.White
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(id = R.drawable.ic_right),
            contentDescription = "열기",
            tint = Color.White,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun TermsDivider() {
    Divider(
        color = Color.White.copy(alpha = 0.4f),
        thickness = 0.5.dp
    )
}