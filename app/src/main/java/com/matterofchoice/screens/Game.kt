package com.matterofchoice.screens


import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.matterofchoice.BottomNav
import com.matterofchoice.Screens
import com.matterofchoice.model.Case
import com.matterofchoice.ui.theme.MatterofchoiceTheme
import com.matterofchoice.viewmodel.AIViewModel


@Composable
fun MainScreen() {
    val navController = rememberNavController()


    Scaffold(
        bottomBar = {
            BottomNav(navController)
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
            composable(Screens.SettingsScreen.screen) { Settings(navController = navController) }
        }
    }
}


@Composable
fun Game(navController: NavHostController) {
    SetUpCase(navController = navController)
}

@Composable
fun SetUpCase(viewmodel: AIViewModel = viewModel(), navController: NavHostController) {
    val listContent by viewmodel.listContent.collectAsState()
    val errorState by viewmodel.errorState.collectAsState()
    val context = LocalContext.current.applicationContext
    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val isFirst = sharedPreferences.getBoolean("isFirst", true)


    if (!isFirst) {
        LaunchedEffect(Unit) {
            viewmodel.initializeCases(context)
        }
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
                val cases: List<Case> = gson.fromJson(content.toString(), listType)
                var selectedItem by remember { mutableStateOf(cases[0].options[0]) }

              if (!cases.isNullOrEmpty()){
                  Column(
                      modifier = Modifier
                          .fillMaxSize()
                          .padding(16.dp)
                          .align(Alignment.CenterHorizontally)
                  ) {
                      Text(text = cases[0].case, color = Color.Red)

                      cases[0].options.forEach {
                          Row(
                              verticalAlignment = Alignment.CenterVertically,
                              modifier = Modifier
                                  .fillMaxWidth()
                                  .height(60.dp)
                                  .selectable(
                                      selected = (selectedItem == it),
                                      onClick = { selectedItem = it },
                                      role = Role.RadioButton
                                  )
                          ) {
                              RadioButton(
                                  selected = (selectedItem == it),
                                  onClick = { selectedItem = it },
                                  modifier = Modifier.padding(end = 16.dp)
                              )
                              Text(it.option)
                          }
                      }
                  }
              }

            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(
                onClick = {
                    navController.navigate(Screens.SettingsScreen.screen)
                }
            ) {
                Text("New Game")
            }
        }
    }


}



@Preview
@Composable
fun MyPreview() {
    val context = LocalContext.current.applicationContext
    MatterofchoiceTheme {
        SetUpCase(navController = NavHostController(context))
    }
}
