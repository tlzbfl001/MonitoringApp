package com.aitronbiz.arron.screen.setting

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.screen.init.LoginActivity
import com.aitronbiz.arron.util.CustomUtil.getUserInfo
import com.aitronbiz.arron.util.CustomUtil.replaceFragment
import com.aitronbiz.arron.viewmodel.MainViewModel

class SettingsFragment : Fragment() {
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SettingsScreen(
                    activity = requireActivity(),
                    viewModel = mainViewModel
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    activity: FragmentActivity,
    viewModel: MainViewModel
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val (name, email) = getUserInfo()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "설정",
                color = Color.White,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                // 사용자 정보
                SettingCard1(
                    title = name.ifBlank { "이름 없음" },
                    subText = email.ifBlank { "이메일 없음" },
                    iconRes = R.drawable.ic_user
                ) {
                    val f = UserInfoFragment()
                    replaceFragment(activity.supportFragmentManager, f, null)
                }
                Spacer(modifier = Modifier.height(11.dp))

                // 서비스 정책
                SettingCard2(title = "서비스 정책") {
                    val f = TermsFragment()
                    replaceFragment(activity.supportFragmentManager, f, null)
                }
                Spacer(modifier = Modifier.height(11.dp))

                // 어플정보
                SettingCard2(title = "어플정보") {
                    val f = AppInfoFragment()
                    replaceFragment(activity.supportFragmentManager, f, null)
                }
                Spacer(modifier = Modifier.height(11.dp))

                // 로그아웃
                SettingCard2(title = "로그아웃") {
                    showLogoutDialog = true
                }
            }
        }
    }

    // 로그아웃 다이얼로그
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.stopTokenAutoRefresh()
                        AppController.prefs.removeToken()
                        AppController.prefs.removeUID()

                        // 로그인 화면으로 완전 초기화 이동
                        val intent = Intent(activity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        activity.startActivity(intent)

                        showLogoutDialog = false
                    }
                ) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("취소") }
            },
            title = { Text("로그아웃") },
            text = { Text("정말 로그아웃 하시겠습니까?") }
        )
    }
}

@Composable
private fun CardContainer(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val fillColor = Color(0x5A185078)
    val strokeColor = Color(0xFF185078)

    Row(
        modifier = modifier
            .clip(shape)
            .background(fillColor)
            .border(width = 1.4.dp, color = strokeColor, shape = shape),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun SettingCard1(
    title: String,
    subText: String? = null,
    iconRes: Int? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(10.dp)
    ) {
        CardContainer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp, end = 20.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                iconRes?.let {
                    Icon(
                        painter = painterResource(id = it),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(80.dp)
                            .padding(end = 20.dp)
                    )
                }
                Column {
                    Text(text = title, color = Color.White, fontSize = 16.sp)
                    subText?.let {
                        Text(text = it, color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_right),
                contentDescription = "$title 이동",
                tint = Color.White,
                modifier = Modifier
                    .padding(end = 15.dp)
                    .size(15.dp)
            )
        }
    }
}

@Composable
private fun SettingCard2(
    title: String,
    subText: String? = null,
    iconRes: Int? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(10.dp)
    ) {
        CardContainer(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp, end = 20.dp, top = 15.dp, bottom = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                iconRes?.let {
                    Icon(
                        painter = painterResource(id = it),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 10.dp)
                    )
                }
                Column {
                    Text(text = title, color = Color.White, fontSize = 16.sp)
                    subText?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = it, color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_right),
                contentDescription = "$title 이동",
                tint = Color.White,
                modifier = Modifier
                    .padding(end = 15.dp)
                    .size(15.dp)
            )
        }
    }
}
