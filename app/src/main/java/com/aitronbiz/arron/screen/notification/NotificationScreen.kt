package com.aitronbiz.arron.screen.notification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.response.NotificationData
import com.aitronbiz.arron.viewmodel.NotificationViewModel

@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = viewModel(),
    token: String
) {
    val notifications by viewModel.notifications.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchNotifications(token)
    }

    val grouped = groupNotificationsByDate(notifications)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .verticalScroll(rememberScrollState())
    ) {
        // 타이틀 바
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 5.dp, end = 20.dp, top = 2.dp, bottom = 6.dp)
        ) {
            IconButton(onClick = {
                val popped = navController.popBackStack()
                if (!popped) navController.navigateUp()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = "알림",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // 날짜별 알림
        grouped.entries.toList().forEachIndexed { index, (date, list) ->
            if (date.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = if (index == 0) 12.dp else 18.dp, // 간격 +3dp
                            bottom = 6.dp,
                            start = 20.dp,
                            end = 20.dp
                        )
                ) {
                    Text(
                        text = date.replace("-", "."),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold)),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                list.forEach { item ->
                    NotificationItem(
                        item = item,
                        onClick = {
                            navController.navigate("notificationDetail/${item.id}")
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (notifications.size >= 30) {
            Text(
                text = "알림은 최대 30개까지 확인할 수 있어요.",
                color = Color.LightGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp, bottom = 40.dp)
            )
        }
    }
}

fun groupNotificationsByDate(notifications: List<NotificationData>): Map<String, List<NotificationData>> {
    return notifications
        .sortedByDescending { it.createdAt }
        .groupBy { it.createdAt?.substringBefore("T") ?: "알 수 없음" }
}

@Composable
fun NotificationItem(
    item: NotificationData,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 11.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x5A185078))
            .border(
                BorderStroke(1.4.dp, Color(0xFF185078)),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                Text(
                    text = item.title ?: "",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold)),
                    fontWeight = if (item.isRead == true) null else FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.body ?: "",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily(Font(R.font.noto_sans_kr_regular)),
                    fontWeight = if (item.isRead == true) null else FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(
                        id = if (item.isRead == true) R.drawable.ic_check else R.drawable.ic_unread_dot
                    ),
                    contentDescription = null,
                    tint = if (item.isRead == true) Color.LightGray else Color(0xFF00BFFF),
                    modifier = Modifier.size(12.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                val time = item.createdAt?.let { parseTime(it) } ?: ""
                Text(
                    text = time,
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
