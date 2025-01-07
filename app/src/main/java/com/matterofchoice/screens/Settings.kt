package com.matterofchoice.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.matterofchoice.Screens
import com.matterofchoice.ui.theme.MyColor

@Composable
fun Settings(navController: NavController) {
    Column {
        Text("Gsdddddd")
        UserInput(navController = navController)
    }


}

@Composable
fun UserInput(
    navController: NavController,
) {
    val context = LocalContext.current.applicationContext
    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)


    var userSubject by remember { mutableStateOf("") }
    var userAge by remember { mutableStateOf("") }
    var userGender by remember { mutableStateOf("") }
    var userLanguage by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp)
        ) {
            OutlinedTextField(
                value = userSubject,
                onValueChange = { userSubject = it },
                colors = OutlinedTextFieldDefaults.colors(MyColor),
                label = { Text(text = "Enter the subject") }

            )

            OutlinedTextField(
                value = userAge,
                onValueChange = { userAge = it },
                colors = OutlinedTextFieldDefaults.colors(MyColor),
                label = { Text(text = "Enter the subject") }

            )

            OutlinedTextField(
                value = userGender,
                onValueChange = { userGender = it },
                colors = OutlinedTextFieldDefaults.colors(MyColor),
                label = { Text(text = "Enter the subject") }

            )

            OutlinedTextField(
                value = userLanguage,
                onValueChange = { userLanguage = it },
                colors = OutlinedTextFieldDefaults.colors(MyColor),
                label = { Text(text = "Enter the subject") }

            )

            Button(
                onClick = {
                    if (userSubject.isBlank() || userAge.isBlank() || userGender.isBlank() || userLanguage.isBlank()) {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("isFirst",false)
                        editor.putString("userSubject", userSubject).apply()
                        editor.putString("userAge", userAge).apply()
                        editor.putString("userGender", userGender).apply()
                        editor.putString("userLanguage", userLanguage).apply()
                        editor.apply()
                        navController.navigate(Screens.GameScreen.screen) {
                            popUpTo(0)
                        }
                    }
                }
            ) {
                Text("Generate Cases")
            }

        }
    }

}

@Preview
@Composable
fun Preview() {
    MaterialTheme {

    }
}


