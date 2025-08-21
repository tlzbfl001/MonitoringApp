package com.aitronbiz.arron.screen.home

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.HomeDTO
import com.aitronbiz.arron.api.dto.HomeDTO2
import com.aitronbiz.arron.api.response.Address
import com.aitronbiz.arron.model.AddressResult
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHomeScreen(
    navController: NavController
) {
    val context = LocalContext.current
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

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val addressResult = savedStateHandle
        ?.getStateFlow<AddressResult?>("address_result", null)
        ?.collectAsState(initial = null)
        ?.value

    // 전달값
    LaunchedEffect(addressResult) {
        addressResult?.let { r ->
            province = r.province
            city = r.city
            street = r.street
            fullAddress = r.fullAddress
            postalCode = r.postalCode
            Log.d(TAG, "province=$province, city=$city, street=$street, full=$fullAddress, postal=$postalCode")
            savedStateHandle?.remove<AddressResult>("address_result")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .pointerInput(Unit) { detectTapGestures { keyboardController?.hide() } }
    ) {
        // 상단 바
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            IconButton(onClick = {
                val popped = navController.popBackStack()
                if (!popped) navController.navigateUp()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text("홈 추가", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "이름",
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.padding(start = 20.dp)
        )
        Spacer(Modifier.height(5.dp))
        WhiteBoxInput(
            value = homeName,
            onValueChange = { if (it.length <= 10) homeName = it },
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
                val hasAddress = fullAddress.isNotBlank()
                val (postalDisplay, postalTextColor) = when {
                    !hasAddress -> "" to hint
                    postalCode.isBlank() -> "우편번호 없음" to hint
                    else -> postalCode to Color.White
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

                CompositionLocalProvider(androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement provides false) {
                    androidx.compose.material3.Button(
                        onClick = { showSearch = true },
                        modifier = Modifier
                            .height(46.dp)
                            .defaultMinSize(minHeight = 1.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF184378),
                            contentColor = Color.White
                        )
                    ) { androidx.compose.material3.Text("주소 검색", fontSize = 14.sp) }
                }
            }

            Spacer(Modifier.height(10.dp))

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

        Button(
            onClick = {
                if (homeName.trim().isEmpty()) {
                    Toast.makeText(context, "홈 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (fullAddress.isBlank()) {
                    Toast.makeText(context, "주소를 검색해 선택하세요.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                scope.launch(Dispatchers.IO) {
                    try {
                        if (postalCode != "") {
                            val dto = HomeDTO(
                                name = homeName.trim(),
                                province = province,
                                city = city,
                                street = street,
                                detailAddress = fullAddress,
                                postalCode = postalCode
                            )

                            val response = RetrofitClient.apiService.createHome(
                                "Bearer ${AppController.prefs.getToken()}",
                                dto
                            )
                            withContext(Dispatchers.Main) {
                                if (response.isSuccessful) {
                                    Log.d(TAG, "createHome: ${response.body()}")
                                    Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                                    val popped = navController.popBackStack()
                                    if (!popped) navController.navigateUp()
                                } else {
                                    Log.e(TAG, "createHome: $response")
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

                            val response = RetrofitClient.apiService.createHome2(
                                "Bearer ${AppController.prefs.getToken()}",
                                dto
                            )
                            withContext(Dispatchers.Main) {
                                if (response.isSuccessful) {
                                    Log.d(TAG, "createHome: ${response.body()}")
                                    Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                                    val popped = navController.popBackStack()
                                    if (!popped) navController.navigateUp()
                                } else {
                                    Log.e(TAG, "createHome: $response")
                                    Toast.makeText(context, "저장 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "에러 발생: ${e.message}", Toast.LENGTH_SHORT).show()
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
        ) { Text("저장", color = Color.White, fontSize = 16.sp) }

        Spacer(Modifier.height(20.dp))
    }

    // 다이얼로그는 어디서 호출해도 중앙 모달로 떠요
    if (showSearch) {
        AddressSearchDialog(
            onDismiss = { showSearch = false },
            onSelected = { r ->
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

@Composable
private fun WhiteBoxInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    hintColor: Color = Color(0xFFBFC7D5),
    textColor: Color = Color.Black,
    height: Dp = 46.dp,
    singleLine: Boolean = true,
) {
    Box(
        modifier = modifier
            .height(height)
            .defaultMinSize(minHeight = 1.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
    ) {
        if (value.isEmpty()) {
            androidx.compose.material3.Text(
                text = placeholder,
                color = hintColor,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 12.dp)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(fontSize = 14.sp, color = textColor),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun OutlineOnlyInput(
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    hintColor: Color = Color(0xFFBFC7D5),
    placeholder: String = "",
    singleLine: Boolean = true,
    height: Dp = 46.dp
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(height)
            .defaultMinSize(minHeight = 1.dp)
            .border(1.dp, Color.White.copy(alpha = 0.85f), shape)
            .padding(horizontal = 12.dp)
    ) {
        if (value.isEmpty()) {
            androidx.compose.material3.Text(
                text = placeholder,
                color = hintColor,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = { if (!readOnly) onValueChange(it) },
            readOnly = readOnly,
            singleLine = singleLine,
            textStyle = TextStyle(fontSize = 14.sp, color = textColor),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSearchDialog(
    onDismiss: () -> Unit,
    onSelected: (AddressResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hint = Color(0xFFBFC7D5)
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<Address>()) }
    var selected by remember { mutableStateOf<Address?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true // 중앙 모달
        )
    ) {
        // material3 Card를 명시적으로 사용 (colors 지원)
        androidx.compose.material3.Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF102A4D))
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                // 헤더
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text(
                        text = "주소 검색",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.TextButton(
                        onClick = onDismiss,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) { androidx.compose.material3.Text("닫기", fontSize = 15.sp) }
                }

                Spacer(Modifier.height(10.dp))

                // 검색 입력 + 버튼 (높이 46dp)
                Row(
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlineOnlyInput(
                        value = query,
                        onValueChange = { query = it },
                        readOnly = false,
                        modifier = Modifier.weight(1f),
                        textColor = Color.White,
                        hintColor = hint,
                        placeholder = "예) 판교역로 235, 성암로 189",
                        height = 46.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    CompositionLocalProvider(
                        androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement provides false
                    ) {
                        FilledIconButton(
                            onClick = {
                                if (query.isBlank()) {
                                    Toast.makeText(context, "검색어를 입력하세요.", Toast.LENGTH_SHORT).show()
                                    results = emptyList()
                                    selected = null
                                    return@FilledIconButton
                                }
                                scope.launch {
                                    val token = "Bearer ${AppController.prefs.getToken()}"
                                    val resp = withContext(Dispatchers.IO) {
                                        RetrofitClient.apiService.searchAddress(token, query.trim())
                                    }
                                    if (resp.isSuccessful) {
                                        results = resp.body()?.addresses ?: emptyList()
                                        selected = null
                                        if (results.isEmpty()) {
                                            Toast.makeText(context, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "검색 실패", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .defaultMinSize(minHeight = 1.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFF184378)
                            )
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.Default.Search,
                                contentDescription = "검색",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 결과 리스트
                if (results.isEmpty() && query.isNotBlank()) {
                    androidx.compose.material3.Text(
                        "검색 결과가 없습니다.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 420.dp)
                    ) {
                        itemsIndexed(results, key = { index, item ->
                            "${item.postalCode}_${item.province}_${item.city}_${item.street}_$index"
                        }) { _, item ->
                            val isSelected = selected == item
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isSelected)
                                            Modifier.border(1.dp, Color.White, RoundedCornerShape(10.dp))
                                        else Modifier
                                    )
                                    .clickable { selected = item }
                                    .padding(vertical = 10.dp, horizontal = 10.dp)
                            ) {
                                val postalStr = item.postalCode?.toString().orEmpty()
                                val full = listOf(
                                    item.province.orEmpty(),
                                    item.city.orEmpty(),
                                    item.street.orEmpty()
                                ).filter { it.isNotBlank() }.joinToString(" ")

                                androidx.compose.material3.Text(
                                    if (postalStr.isNotBlank()) postalStr else "우편번호 없음",
                                    color = Color(0xFFFF5A5A),
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                androidx.compose.material3.Text(
                                    "도로명  $full",
                                    color = Color(0xFF99C7FF),
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                androidx.compose.material3.Text(
                                    "지번    ${item.province.orEmpty()} ${item.city.orEmpty()}",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 확인 버튼 (아래 3dp 패딩)
                androidx.compose.material3.Button(
                    onClick = {
                        val sel = selected ?: run {
                            Toast.makeText(context, "주소를 선택하세요.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val full = listOf(sel.province.orEmpty(), sel.city.orEmpty(), sel.street.orEmpty())
                            .filter { it.isNotBlank() }.joinToString(" ")
                        val postal = extractPostal(sel.postalCode, sel.city, sel.street, full)
                        onSelected(
                            AddressResult(
                                postalCode = postal,
                                fullAddress = full,
                                province = sel.province.orEmpty(),
                                city = sel.city.orEmpty(),
                                street = sel.street.orEmpty()
                            )
                        )
                    },
                    enabled = selected != null,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (selected != null) Color(0xFF184378) else Color(0xFF184378).copy(alpha = 0.5f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(bottom = 3.dp)
                ) {
                    androidx.compose.material3.Text("확인", fontSize = 16.sp)
                }
            }
        }
    }
}

private fun extractPostal(vararg candidates: String?): String {
    candidates.forEach { s ->
        val m = s?.let { Regex("\\b\\d{5}\\b").find(it) }?.value
        if (!m.isNullOrBlank()) return m
    }
    return ""
}