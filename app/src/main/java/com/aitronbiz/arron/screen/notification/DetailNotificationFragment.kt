package com.aitronbiz.arron.screen.notification

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.view.home.MainFragment
import com.aitronbiz.arron.viewmodel.NotificationViewModel

class DetailNotificationFragment : Fragment() {
    private val viewModel: NotificationViewModel by activityViewModels()
    private val token: String = AppController.prefs.getToken().toString()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val notificationId = arguments?.getString("notificationId") ?: ""
                DetailNotificationChartScreen(
                    viewModel = viewModel,
                    token = token,
                    notificationId = notificationId,
                    onBackClick = {
                        replaceFragment1(parentFragmentManager, NotificationFragment())
                    }
                )
            }
        }
    }
}

@Composable
fun DetailNotificationChartScreen(
    viewModel: NotificationViewModel,
    token: String,
    notificationId: String,
    onBackClick: () -> Unit
) {
    val statusBarHeight = detailNotificationBarHeight()
    val notifications by viewModel.notifications.collectAsState()
    val notification = notifications.find { it.id == notificationId }

    // Fragment 진입 시 읽음 처리
    LaunchedEffect(notificationId) {
        if (notificationId.isNotEmpty()) {
            viewModel.markAsRead(token, notificationId) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .padding(top = statusBarHeight + 15.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 상단 타이틀바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.arrow_back),
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.CenterStart)
                    .clickable { onBackClick() }
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

        // 알림 상세 내용
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
            // 데이터 없을 때
            Text(
                text = "알림을 불러올 수 없습니다.",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun detailNotificationBarHeight(): Dp {
    val context = LocalContext.current
    val resourceId = remember {
        context.resources.getIdentifier("status_bar_height", "dimen", "android")
    }
    val heightPx = remember {
        if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }
    return with(LocalDensity.current) { heightPx.toDp() }
}