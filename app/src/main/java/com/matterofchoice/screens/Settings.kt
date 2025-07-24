
package com.matterofchoice.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.matterofchoice.R
import com.matterofchoice.Screens
import com.matterofchoice.common.DropDownMenu
import com.matterofchoice.common.GameButton
import com.matterofchoice.common.GameTextField
import com.matterofchoice.ui.theme.myFont
import java.util.Locale

@Composable
fun Settings(navController: NavController) {
    UserInput(navController = navController)
}

@Composable
fun UserInput(
    navController: NavController,
) {
    val context = LocalContext.current.applicationContext
    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putBoolean("firstOpen", false).apply()

    var userSubject by remember { mutableStateOf("") }
    var userAge by remember { mutableStateOf("") }

    val questionTypes = listOf("Study", "Behavioral", "Hiring")
    val isExposedType = remember { mutableStateOf(false) }

    val subtypesMap = mapOf(
        "Behavioral" to listOf("Interpersonal Skills", "Ethical Dilemmas", "Stress Management"),
        "Study" to listOf("Subject Mastery", "Critical Thinking", "Practical Application"),
        "Hiring" to listOf("Technical Skills", "Behavioral Interview", "Situational Judgment")
    )

    val isExposedSub = remember { mutableStateOf(false) }

    val difficults = listOf("normal", "easy", "hard")
    val isDifficultExposed = remember { mutableStateOf(false) }
    val difficult = remember { mutableStateOf(difficults[0]) }

    val userQuestionType = remember { mutableStateOf(questionTypes[0]) }

    val availableSubtypes = subtypesMap[userQuestionType.value] ?: emptyList()
    val subtype = remember { mutableStateOf(availableSubtypes.firstOrNull() ?: "") }

    LaunchedEffect(userQuestionType.value) {
        val updatedSubtypes = subtypesMap[userQuestionType.value] ?: emptyList()
        subtype.value = updatedSubtypes.firstOrNull() ?: ""
    }

    val deviceLanguage = Locale.getDefault().displayLanguage
    val genders = listOf("", "Male", "Female")
    val languages = context.resources.getStringArray(R.array.languages).toList()
    val sortedLanguages = listOf(deviceLanguage) + languages.filter { it != deviceLanguage }

    val userLanguage = remember { mutableStateOf(sortedLanguages[0]) }
    val userGender = remember { mutableStateOf(genders[0]) }
    val isExposedGender = remember { mutableStateOf(false) }
    val isExposedLanguage = remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Matter of choice",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = myFont,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 15.dp, top = 28.dp)
            )

            Text(
                text = "The scenarios will be based on your selections",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 20.dp)
            )

            GameTextField(
                text = userSubject,
                onValueChange = { userSubject = it },
                labelTxt = "Enter the subject"
            )

            DropDownMenu(questionTypes, isExposedType, userQuestionType, "Select question type")
            DropDownMenu(availableSubtypes, isExposedSub, subtype, "Select the subtype")
            DropDownMenu(difficults, isDifficultExposed, difficult, "Select the difficult level")
            DropDownMenu(genders, isExposedGender, userGender, "Optional: Select your gender")

            GameTextField(
                text = userAge,
                onValueChange = { userAge = it },
                labelTxt = "Enter your Age",
            )

            DropDownMenu(sortedLanguages, isExposedLanguage, userLanguage, "Select your language")

            GameButton(
                onClick = {
                    editor.putBoolean("isFirst", false)
                    editor.putString("userSubject", userSubject).apply()
                    editor.putString("userAge", userAge).apply()
                    editor.putString("userGender", userGender.value).apply()
                    editor.putString("userLanguage", userLanguage.value).apply()
                    editor.putString("userQuestionType", userQuestionType.value).apply()
                    editor.putString("subtype", subtype.value).apply()
                    editor.putString("difficulty", difficult.value).apply()
                    editor.apply()
                    navController.navigate(Screens.GameScreen.screen) {
                        popUpTo(0)
                    }
                },
                text = "Generate Cases"
            )
        }
    }
}
