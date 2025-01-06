package com.matterofchoice.screens

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.matterofchoice.Screens
import com.matterofchoice.model.Case
import com.matterofchoice.ui.theme.MyColor
import com.matterofchoice.viewmodel.AIViewModel

@Composable
fun Settings(viewModel: AIViewModel = viewModel(),navController:NavController) {
    val listContent by viewModel.listContent.collectAsState()
    val errorState by viewModel.errorState.collectAsState()

    UserInput(viewModel, navController = navController)


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        errorState?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }


        listContent?.let { content ->
            val gson = Gson()
            val listType = object : TypeToken<List<Case>>() {}.type
            val cases: List<Case> =
                gson.fromJson(content.toString(), listType)
        }

    }
}

@Composable
fun UserInput(
    viewModel: AIViewModel = viewModel(),
    navController: NavController,
) {
    val context = LocalContext.current.applicationContext
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
//                        val result = viewModel.loadPrompts("prompts.json")
//                        if (result != null) {
//                            viewModel.generateCases(
//                                subject = userSubject,
//                                age = userAge,
//                                gender = userGender,
//                                language = userLanguage,
//                                prompts = result,
//                                context = context
//                            )
                            navController.navigate(Screens.GameScreen.screen) {
                                popUpTo(0)
                            }

//                        else {
//                            viewModel.setError("Failed to load prompts.")
//                        }
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


