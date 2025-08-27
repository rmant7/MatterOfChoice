package com.matterofchoice.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GenerateCasesResponse(val data: List<Case>)

@Serializable
data class Case(
    val answer: String? = null,
    val case: String? = null, // This is the question text
    val case_id: String? = null,
    val optimal: String? = null,
    val options: List<Option>? = null, // List of Option objects
    val turn: Int? = null,
    val user_answer: String? = null,

    // Fields for analysis results (may be null)
    val case_description: String? = null,
    val player_choice: String? = null,
    val optimal_choice: String? = null,
    val analysis: String? = null
)

@Serializable
data class Option(
    val knowledge: Int,
    val number: Int,
    val option: String, // This is the actual option text
    val option_id: String,
    val personal_growth: Int,
    val time_management: Int,
    // Add other fields that might be present
    val health: Int? = 0,
    val wealth: Int? = 0,
    val relationships: Int? = 0,
    val happiness: Int? = 0,
    val karma: Int? = 0,
    val environmental_impact: Int? = 0,
    val social_responsibility: Int? = 0
)

/*
@Composable
fun AnalysisUI(jsonString: String) {
    val systemUiController = rememberSystemUiController()

    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }




    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorScheme.surface
    ) {
        when {
            parseError != null -> {
                Text(
                    text = "Error.",
                    color = colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Overall Judgement",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4B3832)
                        )
                        Text(
                            text = analysisResult!!.overall_judgement,
                            color = Color.Black,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )
                    }

                    items(analysisResult!!.cases) { case ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3F0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Case:",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4B3832)
                                )
                                Text(
                                    text = case.case_description,
                                    color = Color.Black,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Text(
                                    text = "Player's Choice:",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4B3832)
                                )
                                Text(
                                    text = case.player_choice,
                                    color = Color.Black,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Text(
                                    text = "Optimal Choice:",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4B3832)
                                )
                                Text(
                                    text = case.optimal_choice,
                                    color = Color.Black,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Text(
                                    text = "Analysis:",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4B3832)
                                )
                                Text(
                                    text = case.analysis,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}*/
