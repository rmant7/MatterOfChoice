package com.matterofchoice.common

object DropdownMapper {

    // Converts Question Types
    fun getQuestionTypeServerValue(displayText: String): String {
        return when (displayText.lowercase()) {
            "behavioural" -> "behavioural"
            "study" -> "study"
            "recruitment" -> "recruitment"
            else -> displayText.lowercase() // Default: just make it lowercase
        }
    }

    // Converts Difficulty.
    fun getDifficultyServerValue(displayText: String): String {
        return when (displayText.lowercase()) {
            "easy" -> "easy"
            "the normal" -> "the normal"
            "hard" -> "hard"
            else -> displayText.lowercase()
        }
    }

    // Converts Subtype
    fun getSubtypeServerValue(displayText: String): String {
        return when (displayText.lowercase()) {
            "technical skills" -> "technical skills"
            "behavioral interview" -> "behavioral interview"
            "situational judgement" -> "situational judgement"
            "interpersonal skills" -> "interpersonal skills"
            "ethical dilemmas" -> "ethical dilemmas"
            "stress management" -> "stress management"
            "mastery of subjects" -> "mastery of subjects"
            "critical thinking" -> "critical thinking"
            "practical application" -> "practical application"
            else -> displayText.lowercase()
        }
    }

    // Converts Gender
    fun getGenderServerValue(displayText: String): String {
        return when (displayText.lowercase()) {
            "male" -> "male"
            "female" -> "female"
            else -> displayText.lowercase()
        }
    }
}