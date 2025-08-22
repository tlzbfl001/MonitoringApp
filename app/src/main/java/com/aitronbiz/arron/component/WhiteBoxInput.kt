package com.aitronbiz.arron.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WhiteBoxInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    hintColor: Color = Color(0xFFBFC7D5),
    textColor: Color = Color.Black,
    height: Dp = 45.dp,
    singleLine: Boolean = true,
) {
    Box(
        modifier = modifier
            .height(height)
            .defaultMinSize(minHeight = 1.dp)
            .background(Color.White, RoundedCornerShape(10.dp))
    ) {
        if (value.isEmpty()) {
            androidx.compose.material.Text(
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