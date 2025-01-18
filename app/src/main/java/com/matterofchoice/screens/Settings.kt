package com.matterofchoice.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
    editor.putBoolean("firstOpen",false).apply()

    var userSubject by remember { mutableStateOf("") }
    var userAge by remember { mutableStateOf("") }

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
                .padding(top = 24.dp)
                .fillMaxSize()
                ,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Image(
                painter = painterResource(R.drawable.result),
                contentDescription = "",
                modifier = Modifier.size(100.dp)
            )
            Text(text = "Matter of choice", fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = myFont,
                modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 15.dp, top = 28.dp))

            Text(text = "The scenarios will be based on your selections",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 20.dp))

            OutlinedTextField(
                value = userSubject,
                onValueChange = { userSubject = it },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                label = { Text(text = "Enter the subject") }

            )

            OutlinedTextField(
                value = userAge,
                onValueChange = { userAge = it },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                label = { Text(text = "Enter your Age") }

            )


            DropDownMenu(genders, isExposedGender, userGender)
            DropDownMenu(languages, isExposedLanguage, userLanguage)

            val gradientColors = listOf(Color(0xFFFF00CC), Color(0xFF333399))

            Button(
                onClick = {
                    if (userSubject.isBlank() || userAge.isBlank() || userGender.value.isBlank() || userLanguage.value.isBlank()) {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT)
                            .show()
                    } else {
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
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(top = 20.dp).background(
                    brush = Brush.horizontalGradient(gradientColors),
                    shape = RoundedCornerShape(16.dp)
                ),
                colors = ButtonDefaults.buttonColors(Color.Transparent)
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
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            value = selectedItem.value,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExposed.value) },

        )
        ExposedDropdownMenu(
            expanded = isExposed.value,
            onDismissRequest = { isExposed.value = false }) {
            itemsList.forEachIndexed { index, text ->
                DropdownMenuItem(
                    text = { Text(text = text) },
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


