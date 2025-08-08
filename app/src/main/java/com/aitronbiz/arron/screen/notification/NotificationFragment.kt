package com.aitronbiz.arron.screen.notification

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
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
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.SendNotificationDTO
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.api.response.NotificationData
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.getIdFromJwtToken
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.view.home.MainFragment
import com.aitronbiz.arron.viewmodel.NotificationViewModel
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class NotificationFragment : Fragment() {
    private val viewModel: NotificationViewModel by activityViewModels()
    private val token: String = AppController.prefs.getToken().toString()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                NotificationChartScreen(
                    viewModel = viewModel,
                    token = token,
                    fragmentManager = parentFragmentManager,
                    onBackClick = {
                        replaceFragment1(parentFragmentManager, MainFragment())
                    }
                )
            }
        }
    }
}

@Composable
fun NotificationChartScreen(
    viewModel: NotificationViewModel,
    token: String,
    fragmentManager: FragmentManager,
    onBackClick: () -> Unit
) {
    val statusBarHeight = notificationBarHeight()
    val notifications by viewModel.notifications.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchNotifications(token)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .padding(top = statusBarHeight + 15.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 상단바
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
                text = "알림",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold)),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
            Text(
                text = "테스트",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable {
                        val userId = getIdFromJwtToken(AppController.prefs.getToken()!!)!!
                        viewModel.sendTestNotification(token, userId)
                    }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        val grouped = notifications.groupBy {
            it.createdAt?.substring(0, 10) ?: ""
        }

        grouped.forEach { (date, list) ->
            if (date.isNotEmpty()) {
                Text(
                    text = date.replace("-", "."),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold)),
                    modifier = Modifier.padding(start = 20.dp, top = 10.dp)
                )

                list.forEach { item ->
                    NotificationItem(
                        item = item,
                        onClick = {
                            val fragment = DetailNotificationFragment().apply {
                                arguments = Bundle().apply {
                                    putString("notificationId", item.id)
                                }
                            }
                            replaceFragment1(fragmentManager, fragment)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun NotificationItem(
    item: NotificationData,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x5A185078))
            .border(
                BorderStroke(1.4.dp, Color(0xFF185078)),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
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
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold))
                )
                Text(
                    text = item.body ?: "",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                val time = item.createdAt?.let { parseTime(it) } ?: ""
                Text(
                    text = time,
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
                if (item.isRead == true) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "읽음",
                        color = Color.LightGray,
                        fontSize = 10.sp
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

@Composable
fun notificationBarHeight(): Dp {
    val context = LocalContext.current
    val resourceId = remember {
        context.resources.getIdentifier("status_bar_height", "dimen", "android")
    }
    val heightPx = remember {
        if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }
    return with(LocalDensity.current) { heightPx.toDp() }
}