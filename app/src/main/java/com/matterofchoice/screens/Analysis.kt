package com.matterofchoice.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.matterofchoice.R
import com.matterofchoice.api.AnalysisResultResponsee
import com.matterofchoice.api.CaseAnalysise
import com.matterofchoice.common.GameButton
import com.matterofchoice.common.GameTextField
import com.matterofchoice.model.Case
import com.matterofchoice.ui.theme.myFont
import com.matterofchoice.viewmodel.AIViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Analysis(
    viewModel: AIViewModel = viewModel(),
    navController: NavHostController
) {
    val state by viewModel.state
    val context = LocalContext.current

    // Trigger analysis when screen is first shown
    LaunchedEffect(Unit) {
        if (state.analysisData == null && state.userChoices.isNotEmpty()) {
            viewModel.performAnalysis()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.analysis_results)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Loader()
                    }
                }

                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.analysis_error),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Text(
                            text = state.error!!,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                        GameButton(
                            onClick = { viewModel.performAnalysis() },
                            text = stringResource(R.string.retry_analysis)
                        )
                    }
                }

                state.analysisData != null -> {
                    state.casesList?.let {
                        AnalysisResultsUI(
                            analysisData = state.analysisData!!,
                            userChoices = state.userChoices, // Pass user choices
                            cases = it, // Pass cases
                            onRestart = {
                                viewModel.resetGame()
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(R.string.no_analysis_data_available))
                        GameButton(
                            onClick = { viewModel.performAnalysis() },
                            text = stringResource(R.string.perform_analysis)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalysisResultsUI(
    analysisData: AnalysisResultResponsee,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    userChoices: Map<String, String>, // Add user choices to calculate score
    cases: List<Case>, // Add cases to calculate score
) {
    val scrollState = rememberScrollState()
    // Calculate score
    val (correctCount, totalCount, percentage) = calculateSimpleScore(analysisData,)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Score Display at the very top
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.your_score),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "$percentage%",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = stringResource(R.string.correct, correctCount, totalCount),
                    fontSize = 16.sp,
                    color = colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                // Progress bar (optional)
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = percentage / 100f,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = colorScheme.primary,
                    trackColor = colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
            }
        }
        // Overall Assessment
        Text(
            text = stringResource(R.string.overall_assessment),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = analysisData.overall_judgement,
                modifier = Modifier.padding(16.dp),
                fontSize = 16.sp,
                lineHeight = 20.sp
            )
        }

        // Detailed Case Analysis
        Text(
            text = stringResource(R.string.detailed_analysis),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        analysisData.cases.forEachIndexed { index, caseAnalysis ->
            CaseAnalysisItem(
                caseAnalysis = caseAnalysis,
                caseNumber = index + 1,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            GameButton(
                onClick = onBack,
                text = stringResource(R.string.back_to_game),
            )
            GameButton(
                onClick = onRestart,
                text = stringResource(R.string.play_again),
            )
        }
    }
}
private fun calculateSimpleScore(analysisData: AnalysisResultResponsee): Triple<Int, Int, Int> {
    var correctCount = 0
    val totalCount = analysisData.cases.size

    analysisData.cases.forEach { caseAnalysis ->
        if (caseAnalysis.player_choice == caseAnalysis.optimal_choice) {
            correctCount++
        }
    }

    val percentage = if (totalCount > 0) (correctCount * 100 / totalCount) else 0
    return Triple(correctCount, totalCount, percentage)
}
@Composable
fun CaseAnalysisItem(
    caseAnalysis: CaseAnalysise,
    caseNumber: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Case Header
            Text(
                text = stringResource(R.string.case_header, caseNumber),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Case Description
            Text(
                text = caseAnalysis.case_description,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Choices Comparison
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.your_choice),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = caseAnalysis.player_choice,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.optimal_choice),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = caseAnalysis.optimal_choice,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Analysis
            Text(
                text = stringResource(R.string.analysis),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = caseAnalysis.analysis,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun InitialAnalysisUI(
    userRole: String,
    onRoleChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onAsyncAnalyze: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Image(
            painter = painterResource(R.drawable.finish),
            contentDescription = "finish",
            modifier = Modifier.size(100.dp)
        )

        Text(
            text = stringResource(R.string.analysis_your_selections),
            fontFamily = myFont,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 15.dp)
        )

        GameTextField(
            text = userRole,
            onValueChange = onRoleChange,
            labelTxt = stringResource(R.string.optional_enter_the_role_e_g_student)
        )

        Spacer(modifier = Modifier.height(20.dp))

        GameButton(
            onClick = onAnalyze,
            text = stringResource(R.string.analysis_my_choices),

        )

        Spacer(modifier = Modifier.height(10.dp))

        GameButton(
            onClick = onAsyncAnalyze,
            text = "Analyze (Async)",
        )
    }
}

@Composable
fun CurrentCasesUI(
    cases: List<Case>,
    userChoices: Map<String, String>,
    currentTurn: Int,
    currentCaseIndex: Int, // Add this parameter to track which case we're on
    onUserChoice: (String, String) -> Unit,
    onNextCase: () -> Unit, // Separate handler for next case
    onNextTurn: () -> Unit, // Handler for moving to next turn
    onAnalyze: () -> Unit, // Handler for analysis
    onRestart: () -> Unit // Handler for restarting game
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Header with turn information
        Text(
            text = stringResource(
                R.string.turn_of_3_case_of,
                currentTurn,
                currentCaseIndex + 1,
                cases.size
            ),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .align(Alignment.CenterHorizontally)
        )

        // Progress indicator
        Text(
            text = stringResource(R.string.answered, userChoices.size, cases.size),
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .align(Alignment.CenterHorizontally)
        )

        // Display current case only (not all cases)
        if (currentCaseIndex < cases.size) {
            val case = cases[currentCaseIndex]
            val caseId = case.case_id ?: "unknown_${case.hashCode()}"

            CaseCard(
                case = case,
                selectedChoice = userChoices[caseId] ?: "",
                onChoiceSelected = { choice -> onUserChoice(caseId, choice) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Button section - different buttons based on context
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val hasAnswerForCurrentCase = userChoices.containsKey(caseId)

                if (hasAnswerForCurrentCase) {
                    // User has answered current case - show appropriate action buttons
                    if (currentCaseIndex < cases.size - 1) {
                        // More cases in this turn - show "Next Case"
                        GameButton(
                            onClick = onNextCase,
                            text = stringResource(R.string.next_case),
                        )
                    } else {
                        // Last case in turn - show "Next Turn" or "Finish Game"
                        if (currentTurn < 3) {
                            GameButton(
                                onClick = onNextTurn,
                                text = stringResource(R.string.next_turn),
                            )
                        } else {
                            // Game completed - show completion message
                            Text(
                                text = stringResource(R.string.congratulations_you_finished_the_game),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                GameButton(
                                    onClick = onRestart,
                                    text = stringResource(R.string.play_again),
                                )
                                GameButton(
                                    onClick = onAnalyze,
                                    text = stringResource(R.string.analysis),
                                )
                            }
                        }
                    }

                    // Always show Analyze button when user has answered
                    if (currentCaseIndex == cases.size - 1 && currentTurn < 3) {
                        Spacer(modifier = Modifier.height(8.dp))
                        GameButton(
                            onClick = onAnalyze,
                            text = stringResource(R.string.analysis),
                        )
                    }
                } else {
                    // No answer yet - show "New Game" button only
                    GameButton(
                        onClick = onRestart,
                        text = stringResource(R.string.new_game),
                    )
                }
            }
        } else {
            // No cases available
            Text(
                text = "No questions available for this turn",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
                    .align(Alignment.CenterHorizontally)
            )

            GameButton(
                onClick = onRestart,
                text = "New Game",
            )
        }
    }
}

@Composable
fun CaseCard(case: Case, selectedChoice: String, onChoiceSelected: (String) -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Use the "case" field for the question text
            Text(
                text = case.case ?: "No question available",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Display options
            case.options?.forEach { option ->
                val choiceLetter = ('A' + (option.number - 1)).toString() // Convert number to letter
                val isSelected = selectedChoice == choiceLetter

                GameButton(
                    onClick = { onChoiceSelected(choiceLetter) },
                    text = "$choiceLetter. ${option.option}",

                )
            }

            // Show analysis results if available
            case.analysis?.let { analysis ->
                Text(
                    text = stringResource(R.string.analysis_result, analysis),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
@Composable
fun AnalysisCompleteUI(analysisResult: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.finish),
            contentDescription = "finish",
            modifier = Modifier.size(120.dp)
        )

        Text(
            text = stringResource(R.string.game_complete),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        analysisResult?.let { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    text = result,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 16.sp
                )
            }
        } ?: run {
            Text(stringResource(R.string.no_analysis_results_available))
        }

        GameButton(
            onClick = { /* Handle restart or navigation */ },
            text = stringResource(R.string.play_again),
        )
    }
}

@Composable
fun ErrorUI(errorMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.error),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Red,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = errorMessage,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        GameButton(
            onClick = onRetry,
            text = stringResource(R.string.retry),
        )
    }
}
