package com.aitronbiz.arron.screen.device

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.screen.home.SettingHomeFragment
import com.aitronbiz.arron.util.CustomUtil
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.deviceType
import com.aitronbiz.arron.util.CustomUtil.replaceFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoomListFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeId = arguments?.getString("homeId") ?: ""

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RoomListScreen(homeId = homeId)
            }
        }
    }
}

@Composable
fun RoomListScreen(
    homeId: String
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity

    var roomList by remember { mutableStateOf<List<Room>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 룸 정보 불러오기
    LaunchedEffect(homeId) {
        try {
            val res = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getAllRoom(
                    "Bearer ${AppController.prefs.getToken()}",
                    homeId
                )
            }
            if (res.isSuccessful) {
                roomList = res.body()?.rooms ?: emptyList()
            } else {
                Log.e(TAG, "getAllRoom: $res")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
        } finally {
            isLoading = false
        }
    }

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
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            IconButton(onClick = {
                when (deviceType) {
                    1 -> {
                        replaceFragment(context.supportFragmentManager, DeviceFragment(),null)
                    }
                    2  -> {
                        val bundle = Bundle().apply { putString("homeId", homeId) }
                        replaceFragment(context.supportFragmentManager, SettingHomeFragment(), bundle)
                    }
                    else -> context.onBackPressedDispatcher.onBackPressed()
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "장소",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    val bundle = Bundle().apply { putString("homeId", homeId) }
                    replaceFragment(
                        fragmentManager = activity.supportFragmentManager,
                        fragment = AddRoomFragment(),
                        bundle = bundle
                    )
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "추가하기",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(9.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(roomList) { room ->
                        Box(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth()
                                .height(49.dp)
                                .clickable {
                                    val bundle = Bundle().apply {
                                        putString("homeId", homeId)
                                        putString("roomId", room.id)
                                    }
                                    replaceFragment(
                                        fragmentManager = activity.supportFragmentManager,
                                        fragment = SettingRoomFragment(),
                                        bundle = bundle
                                    )
                                }
                                .background(
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    FrameLayout(ctx).apply {
                                        background = ContextCompat.getDrawable(
                                            ctx,
                                            R.drawable.rec_10_blue
                                        )
                                    }
                                },
                                modifier = Modifier.matchParentSize()
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                androidx.compose.material.Text(
                                    text = room.name,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}