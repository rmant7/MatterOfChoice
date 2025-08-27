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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.matterofchoice.common.GameButton
import com.matterofchoice.model.Case
import com.matterofchoice.model.Option
import com.matterofchoice.ui.theme.titleFont
import com.matterofchoice.viewmodel.AIViewModel
import kotlinx.coroutines.launch
import androidx.core.content.edit


@Composable
fun MainScreen(aiViewModel: AIViewModel = viewModel()) {
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
            composable(Screens.GameScreen.screen) { Game(navController, aiViewModel) }
            composable(Screens.ResultScreen.screen) { Result() }
            composable(Screens.AnalysisScreen.screen) { Analysis(aiViewModel,navController = navController) }
            composable(Screens.SettingsScreen.screen) {
                Settings(navController = navController, viewmodel = aiViewModel)
            }
        }
    }
}


@Composable
fun Game(navController: NavHostController, viewmodel: AIViewModel) {
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
    var caseNum by rememberSaveable { mutableIntStateOf(1) }

    val isInitialized by viewmodel.isInitialized.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // This LaunchedEffect runs when the game screen is entered.
    LaunchedEffect(Unit) {
        // Reset scores and rounds for a new game session.
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putInt("userScore", 0)
            putInt("totalScore", 0)
            putInt("rounds", 1)
            apply()
        }
        // Start fetching the cases from the server.
        viewmodel.initiateGame()
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Loader()
        }
    } else if (state.error != null) {
        // Handle error state
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Log.e("SetUpCase", "An error occurred: ${state.error}")
            Text(
                text = state.error ?: stringResource(R.string.error_something_went_wrong),
                fontSize = 18.sp, modifier = Modifier
                    .align(
                        Alignment.CenterHorizontally
                    )
                    .padding(bottom = 20.dp)
            )
            GameButton(
                onClick = { navController.navigate(Screens.SettingsScreen.screen) },
                text = stringResource(R.string.analysis_my_choices)
            )
        }
    } else if (state.casesList != null && state.casesList.isNotEmpty()) {
        // Handle the success state where we have cases
        val scrollState = rememberScrollState()
        val cases = state.casesList

        var selectedItem by remember { mutableStateOf<Option?>(null) }
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)


        // Trigger image generation when the current case changes.
//        LaunchedEffect(caseNum) {
//            if (caseNum - 1 < cases.size) {
//                viewmodel.generateImage(context = context, prompt = cases[caseNum - 1].case)
//            }
//        }

        val round = remember { mutableIntStateOf(sharedPreferences.getInt("rounds", 1)) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header UI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.rounds)+"${round.intValue}",
                    fontFamily = titleFont,
                    textAlign = TextAlign.Justify,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(3.dp)
                )
            }

            // Main content column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
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
                        fontSize = 28.sp,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold
                    )
                    Image(
                        painter = painterResource(R.drawable.fire),
                        modifier = Modifier.size(28.dp),
                        contentDescription = null
                    )
                    val userScore = sharedPreferences.getInt("userScore", 0)
                    val totalScore = sharedPreferences.getInt("totalScore", 0)
                    Text(
                        text = "$userScore / $totalScore",
                        fontFamily = titleFont,
                        fontSize = 18.sp,
                    )
                }

                if (caseNum - 1 < cases.size) {
                    val currentCase = cases[caseNum - 1]
                    Text(
                        text = currentCase.case,
                        fontFamily = titleFont,
                        textAlign = TextAlign.Justify,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                    )

                    // state.image?.let {
                    //     Image(
                    //         bitmap = it.asImageBitmap(),
                    //         contentDescription = null,
                    //         modifier = Modifier
                    //             .padding(bottom = 25.dp, start = 16.dp, end = 16.dp)
                    //             .fillMaxWidth()
                    //             .height(350.dp)
                    //             .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp)),
                    //         contentScale = ContentScale.Crop
                    //     )
                    // }

                    currentCase.options.forEach { option ->
                        OutlinedButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                            onClick = {
                                selectedItem = option
                                coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                            },
                            border = BorderStroke(width = 2.dp, color = if (selectedItem == option) Color.Green else Color.LightGray),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            if (selectedItem == option) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.padding(end = 5.dp), tint = Color.Green)
                            }
                            Text(
                                text = option.option,
                                color = colorScheme.onSurface,
                                fontFamily = titleFont,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column {
                        if (caseNum < cases.size) {
                            GameButton(
                                onClick = {
                                    if (selectedItem != null) {
                                        calculateScore(
                                            cases[caseNum - 1],
                                            selectedItem!!.option,
                                            context
                                        )
                                        viewmodel.onUserChoice(cases[caseNum - 1].case_id, selectedItem!!.option)

                                        round.intValue++
                                        sharedPreferences.edit { putInt("rounds", round.intValue) }
                                        caseNum++
                                        selectedItem = null
                                        coroutineScope.launch {
                                            scrollState.animateScrollTo(0)
                                        }
                                    }
                                },
                                text = stringResource(R.string.button_next)
                            )
                        }
                        GameButton(
                            text = stringResource(R.string.analyze),
                            onClick = {
                                if (selectedItem != null) {
                                    calculateScore(
                                        cases[caseNum - 1],
                                        selectedItem!!.option,
                                        context
                                    )
                                    viewmodel.onUserChoice(cases[caseNum - 1].case_id, selectedItem!!.option)
//                                    viewmodel.performAnalysis()
                                    navController.navigate(Screens.AnalysisScreen.screen)
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    } else {
        // This is the initial state before the user has played a game or if list is empty
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            GameButton(
                onClick = { navController.navigate(Screens.SettingsScreen.screen) },
                text = stringResource(R.string.button_new_game)
            )
        }
    }
}


fun calculateScore(case: Case, selectedOption: String, context: Context) {
    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    // Get the current total scores
    val currentTotalUserScore = sharedPreferences.getInt("userScore", 0)
    val currentTotalOptimalScore = sharedPreferences.getInt("totalScore", 0)

    // Calculate score for the user's choice in the current case
    val userChoice = case.options.find { it.option == selectedOption }
    var currentUserCaseScore = 0
    userChoice?.let {
        currentUserCaseScore = it.health + it.wealth + it.relationships + it.happiness + it.knowledge + it.karma + it.timeManagement +
                it.environmentalImpact + it.personalGrowth + it.socialResponsibility
    }

    // Calculate score for the optimal choice in the current case
    val optimalOption = case.options.find { it.number == case.optimal.toIntOrNull() }
    var currentOptimalCaseScore = 0
    optimalOption?.let {
        currentOptimalCaseScore = it.health + it.wealth + it.relationships + it.happiness + it.knowledge + it.karma + it.timeManagement +
                it.environmentalImpact + it.personalGrowth + it.socialResponsibility
    }

    // Add current case scores to the totals and save
    sharedPreferences.edit {
        putInt("userScore", currentTotalUserScore + currentUserCaseScore)
        putInt("totalScore", currentTotalOptimalScore + currentOptimalCaseScore)
        apply() // Use apply() for asynchronous save
    }

    Log.d("ScoreUpdate", "User Case Score: $currentUserCaseScore, New Total User Score: ${currentTotalUserScore + currentUserCaseScore}")
    Log.d("ScoreUpdate", "Optimal Case Score: $currentOptimalCaseScore, New Total Optimal Score: ${currentTotalOptimalScore + currentOptimalCaseScore}")
}