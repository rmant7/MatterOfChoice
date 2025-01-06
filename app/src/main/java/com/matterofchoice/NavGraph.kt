package com.matterofchoice

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun BottomNav(navController: NavHostController, selected: MutableIntState) {
    BottomAppBar(
        containerColor = Color.White,
        modifier = Modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        )
    ) {
        // Game Icon
        IconButton(
            onClick = {
                selected.intValue = R.drawable.game1
                navController.navigate(Screens.GameScreen.screen) {
                    popUpTo(0) // Ensure we clear previous screens
                }
            }, modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = if (selected.intValue == R.drawable.game1) {
                    painterResource(R.drawable.game1)
                } else {
                    painterResource(R.drawable.game2)
                },
                contentDescription = null
            )
        }

        // Result Icon
        IconButton(
            onClick = {
                selected.intValue = R.drawable.result
                navController.navigate(Screens.ResultScreen.screen)
            }, modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = if (selected.intValue == R.drawable.result) {
                    painterResource(R.drawable.result)
                } else {
                    painterResource(R.drawable.result2)
                },
                contentDescription = null,
            )
        }

        // Settings Icon
        IconButton(
            onClick = {
                selected.intValue = R.drawable.settings1
                navController.navigate(Screens.SettingsScreen.screen)
            }, modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = if (selected.intValue == R.drawable.settings1) {
                    painterResource(R.drawable.settings1)
                } else {
                    painterResource(R.drawable.settings2)
                },
                contentDescription = null,
            )
        }

        // Analysis Icon
        IconButton(
            onClick = {
                selected.intValue = R.drawable.analysis
                navController.navigate(Screens.AnalysisScreen.screen)
            }, modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = if (selected.intValue == R.drawable.analysis) {
                    painterResource(R.drawable.analysis)
                } else {
                    painterResource(R.drawable.analysis2)
                },
                contentDescription = null,
            )
        }
    }
}