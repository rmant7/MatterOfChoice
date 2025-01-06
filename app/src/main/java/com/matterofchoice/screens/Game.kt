package com.matterofchoice.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.matterofchoice.BottomNav
import com.matterofchoice.R
import com.matterofchoice.Screens
import com.matterofchoice.model.Case
import com.matterofchoice.ui.theme.MyColor


private  var caseList:List<Case> = emptyList()

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val selected = remember { mutableIntStateOf(R.drawable.game1) }


    Scaffold(
        bottomBar = {
            BottomNav(navController, selected)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screens.GameScreen.screen,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screens.GameScreen.screen) { Game(navController) }
            composable(Screens.ResultScreen.screen) { Result() }
            composable(Screens.AnalysisScreen.screen) { Analysis() }
            composable(Screens.SettingsScreen.screen) { Settings() }
        }
    }
}


@Composable
fun Game(navController: NavHostController) {
    if (caseList.isEmpty()){
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ){
            Button(
                onClick = {
                    navController.navigate(Screens.SettingsScreen.screen){
                        popUpTo(navController.graph.startDestinationId){saveState = true}
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                shape = RoundedCornerShape(5.dp),
                colors = buttonColors(MyColor)
            ) {
                Text("New Game", fontSize = 18.sp, color = Color.White)
            }
        }
    }else{
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ){
            Button(
                onClick = {

                },
                shape = RoundedCornerShape(5.dp),
                colors = buttonColors(MyColor)
            ) {
                Text("fjfjfj", fontSize = 18.sp, color = Color.White)
            }
        }

    }
}

