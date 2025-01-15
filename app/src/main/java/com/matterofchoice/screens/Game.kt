package com.matterofchoice.screens


import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.matterofchoice.BottomNav
import com.matterofchoice.R
import com.matterofchoice.Screens
import com.matterofchoice.model.Case
import com.matterofchoice.model.Option
import com.matterofchoice.ui.theme.MatterofchoiceTheme
import com.matterofchoice.ui.theme.MyColor
import com.matterofchoice.ui.theme.titleFont
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
fun Loader() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.my_animate))
    val progress by animateLottieCompositionAsState(
        isPlaying = true,
        composition = composition,
        restartOnPlay = true,
        iterations = LottieConstants.IterateForever
    )
    LottieAnimation(
        modifier = Modifier.size(400.dp),
        composition = composition,
        progress = { progress },
    )
}

@Composable
fun SetUpCase(viewmodel: AIViewModel = viewModel(), navController: NavHostController) {
    val context = LocalContext.current.applicationContext


    val isInitialized by viewmodel.isInitialized.collectAsState()
    val listContent by viewmodel.listContent.collectAsState()
    val errorState by viewmodel.errorState.collectAsState()
    val isLoading by viewmodel.isLoading.collectAsState()
    var showLoader by remember { mutableStateOf(true) }

    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val isFirst = sharedPreferences.getBoolean("isFirst", true)


    if (!isFirst) {
        if (!isInitialized) {
            viewmodel.main()
        }
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Generating cases...", fontSize = 18.sp, modifier = Modifier
                        .align(
                            Alignment.TopCenter
                        )
                        .padding(top = 35.dp)
                )
                Loader()
            }
        }
        if (listContent.isNotEmpty()) {
            val scrollState = rememberScrollState()

            Log.v("CASESNOT", "LIST CONTENT IS $listContent")
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxSize()
                    .padding()
                    .verticalScroll(scrollState)
            ) {
                val cases = mutableListOf<Case>()
                val gson = Gson()
                val listType = object : TypeToken<List<Case>>() {}.type

                for (jsonArray in listContent) {
                    val caseList: List<Case> = gson.fromJson(jsonArray.toString(), listType)
                    cases.addAll(caseList)
                }
                Log.v("CASESLIST", cases.toString())

                if (cases.isNotEmpty()) {
                    Log.v("CASESNOT", "Cases is not empty")


                    var selectedItem by remember { mutableStateOf<Option?>(null) }
                    var round by remember { mutableIntStateOf(1) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 4.dp)
                            .background(MyColor)

                    ) {
                        Text(
                            text = "Score: 10/20",
                            fontFamily = titleFont,
                            textAlign = TextAlign.Justify,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(4.dp)

                        )

                    }



                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 16.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {

                        Text(
                            text = "Scenario",
                            fontFamily = titleFont,
                            textAlign = TextAlign.Justify,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = cases[round - 1].case, fontFamily = titleFont,
                            textAlign = TextAlign.Justify,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 30.dp)
                        )
                        showLoader = false

                        Image(
                            painter = painterResource(R.drawable.test), contentDescription = null,
                            modifier = Modifier
                                .padding(bottom = 25.dp)
                                .width(350.dp)
                                .height(350.dp)
                                .align(Alignment.CenterHorizontally)
                                .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )

                        cases[round - 1].options.forEach { option ->
                            OutlinedButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.Start)
                                    .padding(bottom = 12.dp),
                                onClick = {
                                    selectedItem = option
                                },
                                border = BorderStroke(
                                    width = 2.dp,
                                    color = if (selectedItem == option) Color.Green else Color.LightGray
                                ),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                if (selectedItem == option){
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null,
                                        modifier = Modifier
                                            .padding(end = 5.dp), tint = Color.Green)
                                }
                                Text(
                                    text = option.option,
                                    color = Color.Black,
                                    fontFamily = titleFont,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                                )

                            }
                        }
                        Button(
                            onClick = {
                                // implement history
                                if (round < 6 && selectedItem != null) {
                                    Log.v("USERERROR", "User choice: ${selectedItem!!.option}")
                                    calculateScore(cases[round - 1], selectedItem!!.option, context)
//                                    viewmodel.saveUserChoice(
//                                        context,
//                                        listContent!!,
//                                        selectedItem!!.option
//                                    )
                                    round++
                                } else if (round >= 3) {
                                    viewmodel.main()

                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .padding(top = 20.dp)
                                .align(Alignment.CenterHorizontally),
                            colors = ButtonDefaults.buttonColors(MyColor)
                        ) {
                            Text(
                                "Next",
                                fontFamily = titleFont,
                                modifier = Modifier.padding(
                                    start = 20.dp,
                                    end = 20.dp,
                                    top = 5.dp,
                                    bottom = 5.dp
                                ),
                                fontSize = 18.sp
                            )
                        }
                    }

                }
            }

        }
        if (errorState.isNotEmpty()) {
            Log.v("CASESNOT", "Errors is not empty")
            Column(Modifier.fillMaxSize()) {
                Text(
                    text = errorState, fontSize = 18.sp, modifier = Modifier
                        .align(
                            Alignment.CenterHorizontally
                        )
                        .padding(top = 35.dp)
                )

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

fun calculateScore(cases: Case, selectedOption: String, context: Context) {
    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()

    val userChoice =
        cases.options.find { it.option == selectedOption }
    Log.v("USERCALCULATE", "User choice: $userChoice")
    var userScore = 0

    try {
        userChoice!!.apply {
            userScore += health + wealth + relationships + happiness + knowledge + karma + timeManagement +
                    environmentalImpact + personalGrowth + socialResponsibility
        }
        editor.putInt("userScore", userScore)
        editor.apply()

    } catch (e: Exception) {
        Log.v("USERCALCULATE", e.message.toString())
    }


    var totalScore = 0

    val optimalOption =
        cases.options.find { it.number == cases.optimal.toInt() }

    optimalOption!!.apply {
        totalScore += health + wealth + relationships + happiness + knowledge + karma + timeManagement +
                environmentalImpact + personalGrowth + socialResponsibility
    }
    editor.putInt("totalScore", totalScore)
    editor.apply()

}

@Preview
@Composable
fun MyPreview() {
    val context = LocalContext.current.applicationContext
    MatterofchoiceTheme {
        SetUpCase(navController = NavHostController(context))
    }
}

