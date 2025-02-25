package com.matterofchoice.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameButton(
    onClick: () -> Unit,
    text: String,
) {
    val gradientColors = listOf(Color(0xFFFF00CC), Color(0xFF333399))

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .padding(top = 20.dp)
            .background(
                brush = Brush.horizontalGradient(gradientColors),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = ButtonDefaults.buttonColors(Color.Transparent)
    ) {
        Text(
            text = text, modifier = Modifier.padding(
                start = 20.dp,
                end = 20.dp,
                top = 5.dp,
                bottom = 5.dp
            ),
            fontSize = 22.sp
        )


    }

}