package com.matterofchoice

import com.matterofchoice.model.Case

data class GameState(
    val isLoading: Boolean = false,
    val casesList: List<Case>? = null,
    val error: String? = null
)
