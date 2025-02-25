package com.matterofchoice

import android.graphics.Bitmap
import com.matterofchoice.model.Case

data class GameState(
    val isLoading: Boolean = false,
    val casesList: List<Case>? = null,
    val error: String? = null,
    val image: Bitmap? = null
)
