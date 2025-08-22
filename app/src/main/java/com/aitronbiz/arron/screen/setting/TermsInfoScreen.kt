package com.aitronbiz.arron.screen.setting

import com.aitronbiz.arron.R
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun TermsInfoScreen(
    navController: NavController
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
                    text = "서비스 정책",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(13.dp))
        }

        Spacer(Modifier.height(20.dp))

        TermsListItem(
            title = "이용약관",
            onClick = { navController.navigate("terms1") }
        )
        TermsDivider()

        TermsListItem(
            title = "개인정보 수집 및 이용 동의",
            onClick = { navController.navigate("terms2") }
        )
        TermsDivider()

        TermsListItem(
            title = "개인정보 처리방침",
            onClick = { navController.navigate("terms3") }
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
            .padding(horizontal = 20.dp, vertical = 14.dp),
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