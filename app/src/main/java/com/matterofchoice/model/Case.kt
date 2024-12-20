package com.matterofchoice.model

data class Case(
    val case: String,               // The question or scenario
    val options: List<Option>,      // List of possible options
    val optimal: String             // The optimal option's number as a string
)
