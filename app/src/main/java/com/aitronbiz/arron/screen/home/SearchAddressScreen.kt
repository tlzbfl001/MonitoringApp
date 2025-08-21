package com.aitronbiz.arron.screen.home

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon as M2Icon
import androidx.compose.material.IconButton as M2IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Address
import com.aitronbiz.arron.model.AddressResult
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SearchAddressScreen(
    navController: NavController
) {
    val token = "Bearer ${AppController.prefs.getToken()}"
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<Address>()) }
    var selected by remember { mutableStateOf<Address?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF102A4D))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "주소 검색",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                "예) 판교역로 235, 성암로 189",
                                color = Color.LightGray
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                            cursorColor = Color.White,
                            textColor = Color.White,
                            placeholderColor = Color.LightGray
                        )
                    )

                    Spacer(Modifier.width(8.dp))

                    M2IconButton(
                        onClick = {
                            if (query.isBlank()) {
                                Toast.makeText(
                                    navController.context,
                                    "검색어를 입력하세요.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                results = emptyList()
                                selected = null
                                return@M2IconButton
                            }
                            scope.launch {
                                val list = searchAddressApi(token, query.trim())
                                results = list
                                selected = null
                                if (list.isEmpty()) {
                                    Toast.makeText(
                                        navController.context,
                                        "검색 결과가 없습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF184378))
                    ) {
                        M2Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "검색",
                            tint = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 결과 리스트
                if (results.isEmpty() && query.isNotBlank()) {
                    Text(
                        text = "검색 결과가 없습니다.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 420.dp)
                    ) {
                        itemsIndexed(
                            items = results,
                            key = { index, item ->
                                buildString {
                                    append(item.postalCode?.toString() ?: "null")
                                    append('_'); append(item.province ?: "")
                                    append('_'); append(item.city ?: "")
                                    append('_'); append(item.street ?: "")
                                    append('_'); append(index)
                                }
                            }
                        ) { _, item ->
                            AddressResultRow(
                                item = item,
                                selected = selected == item,
                                onClick = { selected = item }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 하단 확인 버튼
                Button(
                    onClick = {
                        val sel = selected
                        if (sel == null) {
                            Toast.makeText(
                                navController.context,
                                "주소를 선택하세요.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        val province = sel.province
                        val cityStr = sel.city
                        val streetStr = sel.street

                        val full = listOf(province, cityStr, streetStr)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")

                        val postal = extractPostal(
                            sel.postalCode,
                            cityStr,
                            streetStr,
                            full
                        )

                        val result = AddressResult(
                            postalCode = postal,
                            fullAddress = full,
                            province   = province,
                            city       = cityStr,
                            street     = streetStr
                        )

                        // AddHomeScreen 으로 전달 후 복귀
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("address_result", result)

                        navController.popBackStack()
                    },
                    enabled = selected != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected != null)
                            Color(0xFF184378)
                        else
                            Color(0xFF184378).copy(alpha = 0.5f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    androidx.compose.material3.Text(text = "확인", fontSize = 16.sp)
                }
            }
        }
    }
}

private suspend fun searchAddressApi(
    token: String,
    query: String
): List<Address> = withContext(Dispatchers.IO) {
    try {
        val resp = RetrofitClient.apiService.searchAddress(token, query)
        if (resp.isSuccessful) {
            Log.d(TAG, "searchAddress: ${resp.body()}")
        } else {
            Log.d(TAG, "searchAddress: $resp")
            return@withContext emptyList()
        }
        resp.body()?.addresses ?: emptyList()
    } catch (t: Throwable) {
        Log.e(TAG, "searchAddress error", t)
        emptyList()
    }
}

// 5자리 우편번호 추출
private fun extractPostal(vararg candidates: String?): String {
    candidates.forEach { s ->
        val m = s?.let { Regex("\\b\\d{5}\\b").find(it) }?.value
        if (!m.isNullOrBlank()) return m
    }
    return ""
}

@Composable
private fun AddressResultRow(
    item: Address,
    selected: Boolean,
    onClick: () -> Unit
) {
    val postalStr = item.postalCode?.toString().orEmpty()
    val cityStr = item.city?.toString().orEmpty()
    val streetStr = item.street?.toString().orEmpty()

    val full = listOf(item.province.orEmpty(), cityStr, streetStr)
        .filter { it.isNotBlank() }
        .joinToString(" ")

    val displayTop = if (postalStr.isNotBlank()) postalStr else "우편번호 없음"
    val displayRoad = "도로명  $full"
    val displayJibun = "지번    ${item.province.orEmpty()} $cityStr"

    val border = if (selected) Color.White else Color.White.copy(alpha = 0.25f)
    val bg = if (selected) Color.White.copy(alpha = 0.06f) else Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(12.dp)
            .border(1.dp, border, RoundedCornerShape(10.dp))
    ) {
        Text(displayTop, color = Color(0xFFFF5A5A), fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            displayRoad,
            color = Color(0xFF99C7FF),
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(displayJibun, color = Color.White, fontSize = 13.sp)
    }
}
