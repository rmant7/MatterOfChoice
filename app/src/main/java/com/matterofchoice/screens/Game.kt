package com.matterofchoice.screens


import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.matterofchoice.BottomNav
import com.matterofchoice.GameState
import com.matterofchoice.R
import com.matterofchoice.Screens
import com.matterofchoice.model.Case
import com.matterofchoice.model.Option
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
            startDestination = Screens.SettingsScreen.screen,
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
    val viewmodel: AIViewModel = viewModel()
    SetUpCase(navController = navController, state = viewmodel.state.value, viewmodel = viewmodel)
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
fun SetUpCase(viewmodel: AIViewModel, navController: NavHostController, state: GameState) {
    val context = LocalContext.current.applicationContext


    val isInitialized by viewmodel.isInitialized.collectAsState()

    val gradientColors = listOf(Color(0xFFFF00CC), Color(0xFF333399))

    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()


    val isFirst = sharedPreferences.getBoolean("isFirst", true)


    if (!isFirst) {
        if (!isInitialized) {
            // could replace with dispose effect
            viewmodel.main()
        }
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Loader()
            }
        }else{
            if (state.casesList != null) {
                val scrollState = rememberScrollState()

                val cases = state.casesList

                Log.v("CASESNOT", "LIST CONTENT IS $cases")
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .fillMaxSize()
                        .padding()
                ) {

                    if (cases.isNotEmpty()) {
                        Log.v("CASESNOT", "Cases is not empty")


                        var selectedItem by remember { mutableStateOf<Option?>(null) }
                        var caseNum by remember { mutableIntStateOf(1) }


                        val userScore = sharedPreferences.getInt("userScore", 0)
                        val totalScore = sharedPreferences.getInt("totalScore", 0)

                        val round =
                            remember { mutableIntStateOf(sharedPreferences.getInt("rounds", 1) + 1) }


                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(elevation = 8.dp)
                                .background(Color.White),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Round ${round.intValue}",
                                fontFamily = titleFont,
                                textAlign = TextAlign.Justify,
                                fontSize = 22.sp,
                                modifier = Modifier.padding(3.dp)
                            )

                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 16.dp)
                                .align(Alignment.CenterHorizontally)
                                .verticalScroll(scrollState)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp, start = 16.dp, end = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Scenario",
                                    fontFamily = titleFont,
                                    textAlign = TextAlign.Justify,
                                    fontSize = 28.sp,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold
                                )
                                Image(
                                    painter = painterResource(R.drawable.fire),
                                    modifier = Modifier.size(28.dp),
                                    contentDescription = null
                                )
                                Text(
                                    text = "$userScore/$totalScore",
                                    fontFamily = titleFont,
                                    textAlign = TextAlign.Justify,
                                    fontSize = 18.sp,

                                    )

                            }


                            Text(
                                text = cases[caseNum - 1].case, fontFamily = titleFont,
                                textAlign = TextAlign.Justify,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                            )

                            Image(
                                painter = painterResource(R.drawable.test), contentDescription = null,
                                modifier = Modifier
                                    .padding(bottom = 25.dp, start = 16.dp, end = 16.dp)
                                    .fillMaxWidth()
                                    .height(350.dp)
                                    .align(Alignment.CenterHorizontally)
                                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )

                            cases[caseNum - 1].options.forEach { option ->
                                OutlinedButton(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.Start)
                                        .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                                    onClick = {
//                                    editor.putInt("rounds", round.intValue++).apply()
                                        selectedItem = option
//                                    if (caseNum < 8 && selectedItem!!.option.isNotEmpty()) {
//                                        calculateScore(
//                                            cases[caseNum - 1],
//                                            selectedItem!!.option,
//                                            context
//                                        )
//                                        viewmodel.saveUserChoice(
//                                            context,
//                                            cases[caseNum - 1],
//                                            selectedItem!!.option
//                                        )
//                                        caseNum++
//
//
//                                    } else {
//                                        viewmodel.main()
//                                    }
                                    },
                                    border = BorderStroke(
                                        width = 2.dp,
                                        color = if (selectedItem == option) Color.Green else Color.LightGray
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                ) {
                                    if (selectedItem == option) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(end = 5.dp),
                                            tint = Color.Green
                                        )
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
                                    if (caseNum < cases.size && selectedItem != null) {
                                        editor.putInt("rounds", round.intValue++).apply()
                                        calculateScore(
                                            cases[caseNum - 1],
                                            selectedItem!!.option,
                                            context
                                        )
                                        viewmodel.saveUserChoice(
                                            context,
                                            cases[caseNum - 1],
                                            selectedItem!!.option
                                        )
                                        caseNum++
                                        selectedItem = null
                                    } else {
                                        viewmodel._state.value = viewmodel._state.value.copy(isLoading = true)
                                        viewmodel.main()

                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .padding(top = 20.dp)
                                    .background(
                                        brush = Brush.horizontalGradient(gradientColors),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                colors = ButtonDefaults.buttonColors(Color.Transparent)
                            )
                            {
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
            state.error?.let {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Log.v("ErrorGEMINI", state.error.toString())
                    Text(
                        text = "Something went wrong", fontSize = 18.sp, modifier = Modifier
                            .align(
                                Alignment.CenterHorizontally
                            )
                            .padding(bottom = 20.dp)
                    )

                    Button(
                        onClick = {
                            navController.navigate(Screens.SettingsScreen.screen)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .background(
                                brush = Brush.horizontalGradient(gradientColors),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(Color.Transparent)
                    ) {
                        Text("New Game")
                    }

                }
            }
        }


    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(
                onClick = {
                    navController.navigate(Screens.SettingsScreen.screen)
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(top = 20.dp)
                    .background(
                        brush = Brush.horizontalGradient(gradientColors),
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = ButtonDefaults.buttonColors(Color.Transparent)
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

