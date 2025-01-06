package com.matterofchoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MatterOfChoiceTheme {
                Surface {
                    val navController = rememberNavController()
                    AppNavHost(navController)
                }
            }
        }
    }
}
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screens.OnboardingScreen.screen) {
        composable(Screens.OnboardingScreen.screen) {
            WelcomeFunction(navController)
        }
        composable(Screens.GameScreen.screen) {
            MainScreen()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    MatterOfChoiceTheme {
        val navController = rememberNavController()
        WelcomeFunction(navController)
    }
}