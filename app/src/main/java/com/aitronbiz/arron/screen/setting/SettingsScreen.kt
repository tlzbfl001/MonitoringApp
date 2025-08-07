package com.aitronbiz.arron.screen.setting

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.view.init.LoginActivity
import com.aitronbiz.arron.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
    ) {
        Text(
            text = "설정",
            fontSize = 20.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 15.dp)
        )

        Spacer(modifier = Modifier.height(15.dp))

        // 나머지 항목 스크롤 가능
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                SettingCard(title = "사용자") {
                    Toast.makeText(context, "사용자 정보 보기", Toast.LENGTH_SHORT).show()
                }
                Spacer(modifier = Modifier.height(11.dp))

                SettingCard(title = "기기 연동") {
                    Toast.makeText(context, "기기 연동 클릭됨", Toast.LENGTH_SHORT).show()
                }
                Spacer(modifier = Modifier.height(11.dp))

                SettingCard(title = "서비스 정책") {
                    Toast.makeText(context, "서비스 정책 클릭됨", Toast.LENGTH_SHORT).show()
                }
                Spacer(modifier = Modifier.height(11.dp))

                SettingCard(title = "모니터링 알림 금지", subText = "-") {
                    Toast.makeText(context, "알림 설정 클릭됨", Toast.LENGTH_SHORT).show()
                }
                Spacer(modifier = Modifier.height(11.dp))

                SettingCard(title = "어플정보") {
                    Toast.makeText(context, "앱 정보 클릭됨", Toast.LENGTH_SHORT).show()
                }
                Spacer(modifier = Modifier.height(11.dp))

                SettingCard(title = "로그아웃") {
                    showLogoutDialog = true
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.stopTokenAutoRefresh()
                        AppController.prefs.removeToken()
                        AppController.prefs.removeUID()

                        Toast.makeText(context, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

                        val intent = Intent(context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)

                        showLogoutDialog = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("취소")
                }
            },
            title = { Text("로그아웃") },
            text = { Text("정말 로그아웃 하시겠습니까?") }
        )
    }
}

@Composable
fun SettingCard(
    title: String,
    subText: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF174176))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, color = Color.White, fontSize = 16.sp)
                subText?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = it, color = Color.LightGray, fontSize = 12.sp)
                }
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_right),
                contentDescription = "$title 이동",
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}