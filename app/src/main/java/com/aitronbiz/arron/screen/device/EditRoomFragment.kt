package com.aitronbiz.arron.screen.device

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.Text as M3Text
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
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.UpdateRoomDTO
import com.aitronbiz.arron.component.WhiteBoxInput
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditRoomFragment : Fragment() {
    private val homeId: String by lazy {
        requireArguments().getString("homeId")
            ?: error("Missing required argument: homeId")
    }
    private val roomId: String by lazy {
        requireArguments().getString("roomId")
            ?: error("Missing required argument: roomId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { EditRoomScreenForFragment(homeId = homeId, roomId = roomId) }
        }
    }
}

@Composable
private fun EditRoomScreenForFragment(
    homeId: String,
    roomId: String,
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    var roomName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val goBack: () -> Unit = {
        val bundle = Bundle().apply {
            putString("homeId", homeId)
            putString("roomId", roomId)
        }
        replaceFragment(
            fragmentManager = activity.supportFragmentManager,
            fragment = SettingRoomFragment(),
            bundle = bundle
        )
    }

    DisposableEffect(Unit) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goBack()
        }
        activity.onBackPressedDispatcher.addCallback(callback)
        onDispose { callback.remove() }
    }

    // 룸 정보 불러오기
    LaunchedEffect(roomId) {
        scope.launch(Dispatchers.IO) {
            try {
                val token = AppController.prefs.getToken()
                val response = RetrofitClient.apiService.getRoom("Bearer $token", roomId)
                if (response.isSuccessful) {
                    val name = response.body()?.room?.name ?: ""
                    withContext(Dispatchers.Main) { roomName = name }
                } else {
                    Log.e(TAG, "getRoom: $response")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getRoom: $e")
            }
        }
    }

    LaunchedEffect(Unit) {
        ViewCompat.setOnApplyWindowInsetsListener(activity.window.decorView) { v, insets ->
            insets
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .statusBarsPadding()
            .pointerInput(Unit) { detectTapGestures { keyboardController?.hide() } }
    ) {
        // 상단 바
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            IconButton(onClick = { goBack() }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "장소 수정",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .weight(1f)
        ) {
            M3Text(text = "장소 이름", color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            WhiteBoxInput(
                value = roomName,
                onValueChange = { if (it.length <= 20) roomName = it },
                placeholder = "예: 거실, 주방",
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = {
                if (roomName.isBlank()) {
                    Toast.makeText(context, "이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                } else {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val token = AppController.prefs.getToken()
                            val dto = UpdateRoomDTO(name = roomName)
                            val response = RetrofitClient.apiService.updateRoom("Bearer $token", roomId, dto)
                            withContext(Dispatchers.Main) {
                                if (response.isSuccessful) {
                                    Log.d(TAG, "updateRoom: ${response.body()}")
                                    Toast.makeText(context, "수정되었습니다.", Toast.LENGTH_SHORT).show()

                                    val bundle = Bundle().apply {
                                        putString("homeId", homeId)
                                        putString("roomId", roomId)
                                    }
                                    replaceFragment(
                                        fragmentManager = activity.supportFragmentManager,
                                        fragment = SettingRoomFragment(),
                                        bundle = bundle
                                    )
                                } else {
                                    Log.e(TAG, "updateRoom: $response")
                                    Toast.makeText(context, "수정 실패하였습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "updateRoom: $e")
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
                containerColor = Color(0xFF184378),
                contentColor = Color.White
            )
        ) {
            M3Text("수정", fontSize = 16.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
