package com.matterofchoice.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.matterofchoice.R
import com.matterofchoice.Screens
import com.matterofchoice.common.DropDownMenu
import com.matterofchoice.common.GameButton
import com.matterofchoice.common.GameTextField
import com.matterofchoice.ui.theme.myFont

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

    val questionTypes = listOf("Study","Behavioral", "Hiring")
    val isExposedType = remember { mutableStateOf(false) }


    val subTypes = listOf("Interpersonal Skills", "Ethical Dilemmas", "Ethical Dilemmas")
    val isExposedSub = remember { mutableStateOf(false) }
    val subtype = remember { mutableStateOf(subTypes[0]) }

    val userQuestionType = remember { mutableStateOf(questionTypes[0]) }
    val genders = listOf("Male", "Female")
    val languages = context.resources.getStringArray(R.array.languages).toList()

    val userLanguage = remember { mutableStateOf(languages[0]) }

    val userGender = remember { mutableStateOf(genders[0]) }

    val isExposedGender = remember { mutableStateOf(false) }

    val isExposedLanguage = remember { mutableStateOf(false) }


    //val gradientColors2 = listOf(Color(0xFFBE93C5), Color(0xFF7BC6CC))


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
            text = "Matter of choice", fontSize = 28.sp,
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
        DropDownMenu(subTypes, isExposedSub, subtype, "Select the subtype")
        DropDownMenu(genders, isExposedGender, userGender, "Optional: Select your gender")


        GameTextField(
            text = userAge,
            onValueChange = { userAge = it },
            labelTxt = "Optional: Enter your Age"
        )

        DropDownMenu(languages, isExposedLanguage, userLanguage, "Select your language")

        GameButton(
            onClick = {
                editor.putBoolean("isFirst", false)
                editor.putString("userSubject", userSubject).apply()
                editor.putString("userAge", userAge).apply()
                editor.putString("userGender", userGender.value).apply()
                editor.putString("userLanguage", userLanguage.value).apply()
                editor.apply()
                navController.navigate(Screens.GameScreen.screen) {
                    popUpTo(0)
                }
            },
            text = "Generate Cases"
        )


    }

}



@Preview(showBackground = true)
@Composable
fun Preview() {
    val context = LocalContext.current.applicationContext
    MaterialTheme {
        UserInput(navController = NavController(context = context))
    }
}


