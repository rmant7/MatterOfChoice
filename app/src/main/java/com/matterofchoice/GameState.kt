package com.matterofchoice

import android.graphics.Bitmap
import com.matterofchoice.model.Case
import com.matterofchoice.screens.AnalysisResult

data class GameState(
    val isLoading: Boolean = false,
    val casesList: List<Case>? = null,
    val error: String? = null,
    val image: Bitmap? = null,
    val analysisResult: AnalysisResult? = null,
    val userChoices: MutableMap<String, String> = mutableMapOf()
)
