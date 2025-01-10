package com.matterofchoice.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.matterofchoice.R
import com.matterofchoice.Screens
import com.matterofchoice.ui.theme.MyBackColor
import com.matterofchoice.ui.theme.MyColor
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

    var userSubject by remember { mutableStateOf("") }
    var userAge by remember { mutableStateOf("") }

    val genders = listOf("Male", "Female")
    val languages = context.resources.getStringArray(R.array.languages).toList()

    val userLanguage = remember { mutableStateOf(languages[0]) }

    val userGender = remember { mutableStateOf(genders[0]) }

    val isExposedGender = remember { mutableStateOf(false) }

    val isExposedLanguage = remember { mutableStateOf(false) }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MyBackColor)
    ) {
        Image(
            painterResource(
                R.drawable.settings_back
            ), contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .padding(24.dp)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(text = "Matter of choice", fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = myFont,
                modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 15.dp))

            Text(text = "The scenarios will be based on your selections",
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 20.dp))

            TextField(
                value = userSubject,
                onValueChange = { userSubject = it },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .shadow(elevation = 2.dp, RoundedCornerShape(10.dp)),
                label = { Text(text = "Enter the subject") }

            )

            TextField(
                value = userAge,
                onValueChange = { userAge = it },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .shadow(elevation = 2.dp, RoundedCornerShape(10.dp)),
                label = { Text(text = "Enter your Age") }

            )


            DropDownMenu(genders, isExposedGender, userGender)
            DropDownMenu(languages, isExposedLanguage, userLanguage)

            Button(
                onClick = {
                    if (userSubject.isBlank() || userAge.isBlank() || userGender.value.isBlank() || userLanguage.value.isBlank()) {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("isFirst", false)
                        editor.putString("userSubject", userSubject).apply()
                        editor.putString("userAge", userAge).apply()
                        editor.putString("userGender", userGender.value).apply()
                        editor.putString("userLanguage", userLanguage.value).apply()
                        editor.apply()
                        navController.navigate(Screens.GameScreen.screen) {
                            popUpTo(0)
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top = 20.dp),
                colors = ButtonDefaults.buttonColors(MyColor)
            ) {
                Text(
                    "Generate Cases",
                    modifier = Modifier.padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 5.dp,
                        bottom = 5.dp
                    ),
                    fontSize = 22.sp
                )
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownMenu(
    itemsList: List<String>,
    isExposed: MutableState<Boolean>,
    selectedItem: MutableState<String>,
) {
    ExposedDropdownMenuBox(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        expanded = isExposed.value,
        onExpandedChange = { isExposed.value = !isExposed.value }
    ) {
        TextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .shadow(elevation = 2.dp, RoundedCornerShape(10.dp)),
            value = selectedItem.value,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExposed.value) },
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color.White, // Change TextField background
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        ExposedDropdownMenu(
            modifier = Modifier.background(Color.White),
            expanded = isExposed.value,
            onDismissRequest = { isExposed.value = false }) {
            itemsList.forEachIndexed { index, text ->
                DropdownMenuItem(
                    text = { Text(text = text, modifier = Modifier.background(Color.White)) },
                    onClick = {
                        selectedItem.value = itemsList[index]
                        isExposed.value = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }

        }

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


