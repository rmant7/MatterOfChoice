package com.matterofchoice

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.matterofchoice.screens.MainScreen
import com.matterofchoice.ui.theme.MatterofchoiceTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MatterofchoiceTheme {
                Surface {
                    val context = LocalContext.current.applicationContext
                    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("isFirst", true)
                    editor.apply()
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
    MatterofchoiceTheme {
        val navController = rememberNavController()
        WelcomeFunction(navController)
    }
}