package com.matterofchoice

import android.graphics.Bitmap
import com.matterofchoice.api.AnalysisResultResponse
import com.matterofchoice.model.Case
import com.matterofchoice.screens.AnalysisResult
import com.matterofchoice.screens.Case

data class GameState(
    val isLoading: Boolean = false,
    val casesList: List<Case>? = null,
    val error: String? = null,
    val image: Bitmap? = null,
    val analysisResult: AnalysisResult? = null,
    val userChoices: MutableMap<String, String> = mutableMapOf(),
    val analysisData: AnalysisResultResponse? = null,
    val gameComplete: Boolean = false, // Added to track game completion
    val currentTurn: Int = 1, // Added to track current turn
    val allCases: List<Case> = emptyList()
)
