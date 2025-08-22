package com.matterofchoice

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import com.matterofchoice.utils.LocaleHelper


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.applyPersistedAppCompatDelegate(applicationContext)

        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate - AFTER super.onCreate.")
        Log.d("MainActivity", "Current configuration locale in Activity: ${resources.configuration.locales[0]}") // THIS IS THE KEY LOG
        Log.d("MainActivity", "Test string from onCreate context: ${getString(R.string.error_something_went_wrong)}")

        enableEdgeToEdge()
        setContent {
            MatterofchoiceTheme {
                Surface() {
                    val context = LocalContext.current.applicationContext
                    Log.d("MainActivity_setContent", "Locale from LocalContext: ${context.resources.configuration.locales[0]}")
                    Log.d("MainActivity_setContent", "Test string from LocalContext: ${context.getString(R.string.error_something_went_wrong)}")

                    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    val isOpen = sharedPreferences.getBoolean("firstOpen",true)
                    val editor = sharedPreferences.edit()
                    editor.apply {
                        putInt("rounds",0)
                        putInt("userScore",0)
                        putInt("totalScore",0)
                    }.apply()
                    editor.putBoolean("isFirst", true)
                    editor.apply()
                    val navController = rememberNavController()
                    AppNavHost(navController, isOpen)
                }
            }
        }
    }


    override fun attachBaseContext(newBase: Context) {
        val configuredContext = LocaleHelper.onAttach(newBase)
        super.attachBaseContext(configuredContext) // Pass the configured context to super
        Log.d("MainActivity", "attachBaseContext - AFTER super.attachBaseContext. Locale from super's context: ${configuredContext.resources.configuration.locales[0]}")
    }

    // Your SettingsScreen will call this when language is changed
    fun recreateActivity() {
        val intent = intent // Get current intent
        finish()
        startActivity(intent)

    }
}
@Composable
fun AppNavHost(navController: NavHostController, isFirstOpen:Boolean) {
    NavHost(navController = navController, startDestination = if(isFirstOpen) Screens.OnboardingScreen.screen else Screens.GameScreen.screen) {
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