package com.aitronbiz.arron.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun EmergencyCallScreen(
    homeId: String,
    roomId: String,
    navController: NavController
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "공사중이에요.",
            color = Color.White,
            fontSize = 17.sp,
            textAlign = TextAlign.Center
        )
    }
}