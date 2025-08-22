package com.aitronbiz.arron.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
fun OutlineOnlyInput(
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