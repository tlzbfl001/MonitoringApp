package com.aitronbiz.arron.screen.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.Button as M2Button
import androidx.compose.material.ButtonDefaults as M2ButtonDefaults
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.ButtonDefaults as M3ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.aitronbiz.arron.api.dto.HomeDTO
import com.aitronbiz.arron.api.dto.HomeDTO2
import com.aitronbiz.arron.component.AddressSearchDialog
import com.aitronbiz.arron.component.OutlineOnlyInput
import com.aitronbiz.arron.component.WhiteBoxInput
import com.aitronbiz.arron.model.AddressResult
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditHomeFragment : Fragment() {
    private val homeId: String by lazy { arguments?.getString("homeId").orEmpty() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { EditHomeScreen(homeId = homeId) }
        }
    }
}

@Composable
private fun EditHomeScreen(
    homeId: String
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val hint = Color(0xFFBFC7D5)
    var homeName by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    // 주소 상태
    var province by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var street by rememberSaveable { mutableStateOf("") }
    var fullAddress by rememberSaveable { mutableStateOf("") }
    var postalCode by rememberSaveable { mutableStateOf("") }

    // 초기 로드
    LaunchedEffect(homeId) {
        if (homeId.isBlank()) return@LaunchedEffect
        try {
            val token = "Bearer ${AppController.prefs.getToken()}"
            val resp = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getHome(token, homeId)
            }
            if (resp.isSuccessful) {
                resp.body()?.home?.let { h ->
                    homeName = h.name.orEmpty()
                    province = h.province.orEmpty()
                    city = h.city.orEmpty()
                    street = h.street.orEmpty()
                    fullAddress = h.detailAddress.orEmpty()
                    postalCode = h.postalCode.orEmpty()
                }
            } else {
                Toast.makeText(context, "홈 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "getHome: $resp")
            }
        } catch (e: Exception) {
            Toast.makeText(context, "홈 로드 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "getHome error", e)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            IconButton(onClick = { activity.onBackPressedDispatcher.onBackPressed() }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(Modifier.width(4.dp))
            Text("홈 수정", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Text(
            "이름",
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.padding(start = 20.dp)
        )
        Spacer(Modifier.height(5.dp))
        WhiteBoxInput(
            value = homeName,
            onValueChange = { if (it.length <= 20) homeName = it },
            placeholder = "홈 이름을 입력하세요",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            hintColor = hint,
            height = 46.dp
        )

        Spacer(Modifier.height(25.dp))

        // 주소
        Text(
            "주소",
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.padding(start = 20.dp)
        )

        Spacer(Modifier.height(5.dp))

        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (postalDisplay, postalTextColor) = when {
                    postalCode.isNotBlank() -> postalCode to Color.White
                    fullAddress.isNotBlank() -> "우편번호 없음" to hint
                    else -> "" to hint
                }

                OutlineOnlyInput(
                    value = postalDisplay,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    textColor = postalTextColor,
                    hintColor = hint,
                    placeholder = "우편번호",
                    height = 46.dp
                )

                Spacer(Modifier.width(8.dp))

                M3Button(
                    onClick = { showSearch = true },
                    modifier = Modifier
                        .height(46.dp)
                        .defaultMinSize(minHeight = 1.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = M3ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF184378),
                        contentColor = Color.White
                    )
                ) { androidx.compose.material3.Text("주소 검색", fontSize = 14.sp) }
            }

            Spacer(Modifier.height(10.dp))

            // 상세주소
            OutlineOnlyInput(
                value = fullAddress,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                textColor = if (fullAddress.isBlank()) hint else Color.White,
                hintColor = hint,
                placeholder = "상세주소",
                singleLine = false,
                height = 46.dp
            )
        }

        Spacer(Modifier.weight(1f))

        M2Button(
            onClick = {
                if (homeName.trim().isEmpty()) {
                    Toast.makeText(context, "홈 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                    return@M2Button
                }
                if (fullAddress.isBlank()) {
                    Toast.makeText(context, "주소를 검색해 선택하세요.", Toast.LENGTH_SHORT).show()
                    return@M2Button
                }

                scope.launch(Dispatchers.IO) {
                    try {
                        val token = "Bearer ${AppController.prefs.getToken()}"
                        if (postalCode.isNotBlank()) {
                            val dto = HomeDTO(
                                name = homeName.trim(),
                                province = province,
                                city = city,
                                street = street,
                                detailAddress = fullAddress,
                                postalCode = postalCode
                            )
                            val response = RetrofitClient.apiService.updateHome(token, homeId, dto)
                            withContext(Dispatchers.Main) {
                                if (response.isSuccessful) {
                                    Log.d(TAG, "updateHome: ${response.body()}")
                                    Toast.makeText(context, "수정되었습니다.", Toast.LENGTH_SHORT).show()

                                    val b = Bundle().apply { putString("homeId", homeId) }
                                    replaceFragment2(
                                        fragmentManager = activity.supportFragmentManager,
                                        fragment = SettingHomeFragment(),
                                        bundle = b
                                    )
                                } else {
                                    Log.e(TAG, "updateHome: $response")
                                    Toast.makeText(context, "저장 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            val dto = HomeDTO2(
                                name = homeName.trim(),
                                province = province,
                                city = city,
                                street = street,
                                detailAddress = fullAddress
                            )
                            val response = RetrofitClient.apiService.updateHome2(token, homeId, dto)
                            withContext(Dispatchers.Main) {
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "수정되었습니다.", Toast.LENGTH_SHORT).show()

                                    val b = Bundle().apply { putString("homeId", homeId) }
                                    replaceFragment2(
                                        fragmentManager = activity.supportFragmentManager,
                                        fragment = SettingHomeFragment(),
                                        bundle = b
                                    )
                                } else {
                                    Log.e(TAG, "updateHome: $response")
                                    Toast.makeText(context, "저장 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "에러 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        Log.e(TAG, "updateHome error", e)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(30.dp),
            colors = M2ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF184378),
                contentColor = Color.White
            )
        ) { Text("저장", color = Color.White, fontSize = 16.sp) }

        Spacer(Modifier.height(20.dp))
    }

    if (showSearch) {
        AddressSearchDialog(
            onDismiss = { showSearch = false },
            onSelected = { r: AddressResult ->
                province = r.province
                city = r.city
                street = r.street
                fullAddress = r.fullAddress
                postalCode = r.postalCode
                showSearch = false
            }
        )
    }
}
