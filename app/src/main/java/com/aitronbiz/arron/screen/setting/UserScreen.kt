package com.aitronbiz.arron.screen.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.viewmodel.MainViewModel
import com.aitronbiz.arron.viewmodel.NotificationViewModel

@Composable
fun UserScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.fetchUserSession()
    }
    val userData by viewModel.userData

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
                onClick = {
                    val popped = navController.popBackStack()
                    if (!popped) navController.navigateUp()
                },
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
                    text = "사용자",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(13.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_user),
                contentDescription = "User",
                tint = Color.Unspecified,
                modifier = Modifier.size(96.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = userData.name.ifBlank { "이름 없음" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = userData.email.ifBlank { "이메일 없음" },
                fontSize = 14.sp,
                color = Color(0xFFBDD0E8)
            )
        }
    }
}
