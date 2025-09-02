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
            "technical_skills" -> "technical_skills"
            "behavioral_interview" -> "behavioral_interview"
            "situational_judgement" -> "situational_judgement"
            "interpersonal_skills" -> "interpersonal_skills"
            "ethical_dilemmas" -> "ethical_dilemmas"
            "stress_management" -> "stress_management"
            "mastery_of_subjects" -> "mastery_of_subjects"
            "critical_thinking" -> "critical_thinking"
            "practical_application" -> "practical_application"
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