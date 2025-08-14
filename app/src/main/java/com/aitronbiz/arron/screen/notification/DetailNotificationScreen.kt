package com.aitronbiz.arron.screen.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.viewmodel.NotificationViewModel
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DetailNotificationScreen(
    notificationId: String,
    token: String,
    navController: NavController,
    viewModel: NotificationViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val notification = notifications.find { it.id == notificationId }

    // 읽음 처리
    LaunchedEffect(notificationId) {
        if (notificationId.isNotEmpty()) {
            viewModel.markAsRead(token, notificationId) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.arrow_back),
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.CenterStart)
                    .clickable { navController.popBackStack() }
            )
            Text(
                text = "알림 상세",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold)),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        if (notification != null) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = notification.title ?: "",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold))
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = notification.body ?: "",
                    color = Color(0xFFB0C4DE),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                val time = notification.createdAt?.let { parseTime(it) } ?: ""
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = time,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            Text(
                text = "알림을 불러올 수 없습니다.",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

fun parseTime(utcDateTime: String): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
        val utcZoned = ZonedDateTime.parse(utcDateTime, formatter.withZone(ZoneOffset.UTC))
        val localZoned = utcZoned.withZoneSameInstant(ZoneId.systemDefault())
        val timeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.getDefault())
        localZoned.format(timeFormatter)
    } catch (e: Exception) {
        ""
    }
}