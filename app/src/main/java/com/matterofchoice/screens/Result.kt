package com.matterofchoice.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matterofchoice.ui.theme.MatterofchoiceTheme

@Composable
fun Result() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)

    val userScore = sharedPreferences.getInt("userScore", 0)
    val totalScore = sharedPreferences.getInt("totalScore", 0)

    Box(modifier = Modifier.fillMaxSize().background(Color.White).padding(16.dp)) {
        Text(text = "Your score", fontSize = 38.sp,modifier = Modifier.align(Alignment.TopCenter)
            .padding(top = 100.dp))
        Column(modifier = Modifier.align(Alignment.Center).padding(bottom = 250.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$userScore / $totalScore", fontSize = 56.sp, color = Color.Red)
        }
    }
}



@Preview
@Composable
fun MyPreView() {
    MatterofchoiceTheme {
        Result()
    }
}