package com.matterofchoice

import AnalysisResult
import android.graphics.Bitmap
import com.matterofchoice.api.AnalysisResultResponsee
import com.matterofchoice.model.Case

data class GameState(
    val isLoading: Boolean = false,
    val casesList: List<Case>? = null,
    val error: String? = null,
    val image: Bitmap? = null,
    val analysisResult: AnalysisResult? = null,
    val userChoices: MutableMap<String, String> = mutableMapOf(),
    val analysisData: AnalysisResultResponsee? = null,
    val gameComplete: Boolean = false, // Added to track game completion
    val currentTurn: Int = 1, // Added to track current turn
    val allCases: List<Case> = emptyList(),
    val analysisResulte: String? = null,
)
