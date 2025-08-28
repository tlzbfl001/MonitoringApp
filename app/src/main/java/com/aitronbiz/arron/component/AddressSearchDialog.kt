package com.aitronbiz.arron.component

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Address
import com.aitronbiz.arron.model.AddressResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        androidx.compose.material3.Card(
            shape = RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = Color(0xFF102A4D)
            )
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    androidx.compose.material3.Text(
                        text = "주소 검색",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    androidx.compose.material3.TextButton(
                        onClick = onDismiss,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        androidx.compose.material3.Text("닫기", fontSize = 15.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 검색 입력 + 돋보기 버튼
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp),
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
                    androidx.compose.material3.FilledIconButton(
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
                        colors = androidx.compose.material3.IconButtonDefaults
                            .filledIconButtonColors(containerColor = Color(0xFF184378))
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Default.Search,
                            contentDescription = "검색",
                            tint = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (results.isEmpty() && query.isNotBlank()) {
                    androidx.compose.material3.Text(
                        "검색 결과가 없습니다.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 550.dp)
                    ) {
                        itemsIndexed(
                            results,
                            key = { index, item ->
                                "${item.postalCode}_${item.province}_${item.city}_${item.street}_$index"
                            }
                        ) { _, item ->
                            val isSelected = selected == item
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isSelected)
                                            Modifier.border(
                                                1.dp,
                                                Color.White,
                                                RoundedCornerShape(10.dp)
                                            )
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

                androidx.compose.material3.Button(
                    onClick = {
                        val sel = selected ?: run {
                            Toast.makeText(context, "주소를 선택하세요.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val full = listOf(
                            sel.province.orEmpty(),
                            sel.city.orEmpty(),
                            sel.street.orEmpty()
                        ).filter { it.isNotBlank() }.joinToString(" ")
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
                        containerColor = Color(0xFF184378),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0x9A314054),
                        disabledContentColor = Color.White.copy(alpha = 0.75f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(bottom = 16.dp)
                ) {
                    androidx.compose.material3.Text("확인", fontSize = 15.sp)
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