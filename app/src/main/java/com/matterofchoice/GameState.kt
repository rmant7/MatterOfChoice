package com.matterofchoice

import android.graphics.Bitmap
import com.matterofchoice.api.AnalysisResultResponse
import com.matterofchoice.screens.Case

data class GameState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val casesList: List<Case> = emptyList(), // Changed from nullable to emptyList default
    val userChoices: Map<String, String> = emptyMap(), // Changed from MutableMap to immutable Map
    val analysisResult: String? = null,
    val analysisData: AnalysisResultResponse? = null,
    val gameComplete: Boolean = false, // Added to track game completion
    val currentTurn: Int = 1, // Added to track current turn
    val image: Bitmap? = null, // Kept for backward compatibility if needed
    val allCases: List<Case> = emptyList() // Store all cases from all turns
)
