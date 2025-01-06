package com.matterofchoice.model


data class Case(
    val case: String,
    val options: List<Option>,
    val optimal: String
)