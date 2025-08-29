package com.aitronbiz.arron.screen.device

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.RoomDTO
import com.aitronbiz.arron.component.WhiteBoxInput
import com.aitronbiz.arron.screen.home.SettingHomeFragment
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.layoutType
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddRoomFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeId = arguments?.getString("homeId") ?: ""

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AddRoomScreen(homeId = homeId)
            }
        }
    }
}

@Composable
private fun AddRoomScreen(
    homeId: String
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    var roomName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    fun replaceFragment() {
        when(layoutType) {
            1 -> replaceFragment2(activity.supportFragmentManager, DeviceFragment(), null)
            2 -> {
                val bundle = Bundle().apply { putString("homeId", homeId) }
                replaceFragment2(activity.supportFragmentManager, SettingHomeFragment(), bundle)
            }
            else -> activity.onBackPressedDispatcher.onBackPressed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .windowInsetsPadding(WindowInsets.statusBars)
            .pointerInput(Unit) {
                detectTapGestures { keyboardController?.hide() }
            }
    ) {
        // 상단 바
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            IconButton(onClick = {
                replaceFragment()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            androidx.compose.material3.Text(
                text = "장소 추가",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "장소 이름",
            fontSize = 15.sp,
            color = Color.White,
            modifier = Modifier.padding(start = 20.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        WhiteBoxInput(
            value = roomName,
            onValueChange = { if (it.length <= 20) roomName = it },
            placeholder = "예: 거실, 주방",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                when {
                    roomName.trim().isEmpty() -> {
                        Toast.makeText(context, "장소 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                    }
                    homeId.isBlank() -> {
                        Toast.makeText(context, "홈 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val dto = RoomDTO(name = roomName.trim(), homeId = homeId)
                                val response = RetrofitClient.apiService.createRoom(
                                    "Bearer ${AppController.prefs.getToken()}",
                                    dto
                                )
                                withContext(Dispatchers.Main) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                                        replaceFragment()
                                    } else {
                                        Log.e(TAG, "createRoom: $response")
                                        Toast.makeText(context, "저장 실패하였습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "createRoom error", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "에러 발생", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF184378),
                contentColor = Color.White
            )
        ) {
            Text("저장", color = Color.White, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}