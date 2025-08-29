package com.aitronbiz.arron.screen.notification

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aitronbiz.arron.R
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.viewmodel.NotificationViewModel
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class NotificationDetailFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationId = arguments?.getString("notificationId").orEmpty()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DetailNotificationScreen(notificationId = notificationId)
            }
        }
    }
}

@Composable
private fun DetailNotificationScreen(
    notificationId: String,
    viewModel: NotificationViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity

    val notifications by viewModel.notifications.collectAsState()
    val notification = notifications.find { it.id == notificationId }

    // 읽음 처리
    LaunchedEffect(notificationId) {
        if (notificationId.isNotEmpty()) {
            viewModel.markAsRead(notificationId) {  }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .verticalScroll(rememberScrollState())
    ) {
        // 상단바
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 5.dp, end = 20.dp, top = 3.dp, bottom = 6.dp)
        ) {
            IconButton(onClick = { activity.onBackPressedDispatcher.onBackPressed() }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = "알림 상세",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (notification != null) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = notification.title.orEmpty(),
                    color = Color(0xFFADADAD),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = notification.body.orEmpty(),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(20.dp))
                val time = notification.createdAt?.let { parseTime(it) }.orEmpty()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = time,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
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