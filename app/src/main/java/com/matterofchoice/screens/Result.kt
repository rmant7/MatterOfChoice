package com.matterofchoice.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matterofchoice.R
import com.matterofchoice.ui.theme.MatterofchoiceTheme

@Composable
fun Result() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    var userScore = 0
    var totalScore = 0

        userScore = sharedPreferences.getInt("userScore", 0)
        totalScore = sharedPreferences.getInt("totalScore", 0)

        Log.v("USERSCORE", userScore.toString())
        Log.v("TOTALSCORE", totalScore.toString())



    Column(
        modifier = Modifier
            .padding(bottom = 150.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painterResource(R.drawable.result3),
            contentDescription = null,
            modifier = Modifier
                .size(200.dp),
            contentScale = ContentScale.Crop

        )

            Text(
                text = "Your score",
                fontSize = 38.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 70.dp)
            )
            Text(
                text = "$userScore / $totalScore",
                fontSize = 38.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = Color.Yellow
            )
    }

}


@Preview
@Composable
fun MyPreView() {
    MatterofchoiceTheme {
        Result()
    }
}