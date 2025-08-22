package com.matterofchoice.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.matterofchoice.GameState
import com.matterofchoice.api.ModalApiClient
import com.matterofchoice.screens.Case
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException

class AIViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences =
        application.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    private var i = 0 // Used for saving files
    private var currentTurn = 1 // Track the current game turn

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> get() = _isInitialized

    private var _state = mutableStateOf(GameState())
    var state: State<GameState> = _state

    /**
     * Initialize the session before starting the game
     */
    fun initializeSession() {
        viewModelScope.launch {
            try {
                val success = ModalApiClient.initializeSession()
                if (success) {
                    _isInitialized.value = true
                    Log.d("AIViewModel", "Session initialized successfully")
                } else {
                    _state.value = _state.value.copy(error = "Failed to initialize session")
                }
            } catch (e: Exception) {
                Log.e("AIViewModel", "Session initialization failed", e)
                _state.value = _state.value.copy(error = "Session init failed: ${e.message}")
            }
        }
    }

    /**
     * This is the main entry point to start the game.
     * Call this from your Composable or Activity when the user is ready.
     */
    fun initiateGame() {
        if (state.value.isLoading) {
            Log.w("AIViewModel", "Game initiation already in progress. Ignoring new request.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                // 1. Get user preferences from SharedPreferences
                val userSubject = sharedPreferences.getString("userSubject", "life skills")!!
                val userAge = sharedPreferences.getString("userAge", "25")!!
                val userGender = sharedPreferences.getString("userGender", "any")!!
                val userLanguage = sharedPreferences.getString("userLanguage", "English")!!

                // 2. Generate cases with retry logic
                Log.d("AIViewModel", "Generating cases for turn ${_state.value.currentTurn}...")
                val cases = ModalApiClient.generateCasesWithRetry(
                    language = userLanguage,
                    subject = userSubject,
                    difficulty = "medium",
                    questionType = "behavioral",
                    subType = "scenario_analysis",
                    age = userAge.toIntOrNull() ?: 25,
                    sex = userGender,
                    role = "Parent",
                    answers = if (_state.value.currentTurn > 1) _state.value.userChoices else null
                )

                // 3. Update state with new cases
                _state.value = _state.value.copy(
                    isLoading = false,
                    casesList = cases,
                    error = null
                )
                Log.i("AIViewModel", "Turn ${_state.value.currentTurn} complete! Received ${cases.size} cases.")

            } catch (e: Exception) {
                Log.e("AIViewModel", "Failed to generate cases for turn ${_state.value.currentTurn}", e)
                _state.value = _state.value.copy(isLoading = false, error = "Failed to generate cases: ${e.message}")
            }
        }
    }

    /**
     * Proceed to the next turn in the game
     */
    fun nextTurn() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                // Generate cases for next turn
                val userSubject = sharedPreferences.getString("userSubject", "life skills")!!
                val userAge = sharedPreferences.getString("userAge", "25")!!
                val userGender = sharedPreferences.getString("userGender", "any")!!
                val userLanguage = sharedPreferences.getString("userLanguage", "English")!!

                val cases = ModalApiClient.generateCasesWithRetry(
                    language = userLanguage,
                    subject = userSubject,
                    difficulty = "medium",
                    questionType = "behavioral",
                    subType = "scenario_analysis",
                    age = userAge.toIntOrNull() ?: 25,
                    sex = userGender,
                    role = "Parent",
                    answers = _state.value.userChoices // Send all previous answers
                )

                _state.value = _state.value.copy(
                    isLoading = false,
                    casesList = cases,
                    currentTurn = _state.value.currentTurn + 1,
                    allCases = _state.value.allCases + cases,
                    error = null
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load next turn: ${e.message}"
                )
            }
        }
    }

    fun onUserChoice(caseId: String, choice: String) {
        val currentChoices = _state.value.userChoices.toMutableMap()
        currentChoices[caseId] = choice
        _state.value = _state.value.copy(userChoices = currentChoices)
        Log.d("AIViewModel", "User choice recorded: $caseId -> $choice")
    }

    /**
     * Perform final analysis after all turns are complete
     */
    // In your AIViewModel
    fun performAnalysis() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                // Get user preferences
                val userLanguage = sharedPreferences.getString("userLanguage", "English")!!

                // Use synchronous analysis
                val analysisResult = ModalApiClient.submitResponses(
                    answers = _state.value.userChoices,
                    role = "Parent", // or get from user preferences
                    questionType = "behavioral", // or get from user preferences
                    subType = "scenario_analysis", // or get from user preferences
                    language = userLanguage
                )

                _state.value = _state.value.copy(
                    analysisResult = analysisResult.overall_judgement,
                    isLoading = false,
                    analysisData = analysisResult // Store the full analysis data
                )

            } catch (e: Exception) {
                Log.e("AIViewModel", "Analysis failed", e)
                _state.value = _state.value.copy(
                    error = "Analysis failed: ${e.message}",
                    isLoading = false
                )
            }
        }
    }



    /**
     * Alternative: Perform analysis asynchronously (if you prefer this approach)
     */

// Alternative: Async analysis
    fun performAsyncAnalysis() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val userLanguage = sharedPreferences.getString("userLanguage", "English")!!

                // Start async analysis job
                val jobId = ModalApiClient.startAnalysis(
                    role = "Parent",
                    questionType = "behavioral",
                    language = userLanguage
                )

                // Poll for results
                var status = ModalApiClient.getAnalysisStatus(jobId)
                var pollCount = 0
                val maxPolls = 30 // 30 attempts with 2-second delay = 60 seconds max

                while (status.status == "pending" && pollCount < maxPolls) {
                    delay(2000)
                    status = ModalApiClient.getAnalysisStatus(jobId)
                    pollCount++
                }

                when (status.status) {
                    "complete" -> {
                        val result = status.result
                        _state.value = _state.value.copy(
                            analysisResult = result?.overall_judgement ?: "Analysis complete",
                            analysisData = result,
                            isLoading = false
                        )
                    }
                    "failed" -> {
                        _state.value = _state.value.copy(
                            error = "Analysis failed: ${status.error}",
                            isLoading = false
                        )
                    }
                    else -> {
                        _state.value = _state.value.copy(
                            error = "Analysis timed out",
                            isLoading = false
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("AIViewModel", "Async analysis failed", e)
                _state.value = _state.value.copy(
                    error = "Analysis failed: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    /**
     * Reset the game to start over
     */
    fun resetGame() {
        viewModelScope.launch {
            try {
                ModalApiClient.resetSession()
                currentTurn = 1
                _state.value = GameState() // Reset to initial state
                _isInitialized.value = false
                Log.d("AIViewModel", "Game reset successfully")
            } catch (e: Exception) {
                Log.e("AIViewModel", "Failed to reset game", e)
                _state.value = _state.value.copy(error = "Reset failed: ${e.message}")
            }
        }
    }

    /**
     * Chat with the AI
     */
    fun converseWithAI(message: String) {
        viewModelScope.launch {
            try {
                val response = ModalApiClient.converse(message)
                // Handle the AI response - you might want to add this to your GameState
                Log.d("AIViewModel", "AI response: $response")
                // You could update state with the conversation history
            } catch (e: Exception) {
                Log.e("AIViewModel", "AI conversation failed", e)
            }
        }
    }

    /**
     * Saves the user's choice for a given case to a local file.
     * This logic remains unchanged.
     */
    fun saveUserChoice(context: Context, caseData: Case, userChoice: String) {
        val outputPath = context.getExternalFilesDir("tool")?.absolutePath ?: ""
        val caseFile = File(outputPath, "option_$i.json")
        i++ // Increment file index

        if (outputPath.isEmpty()) {
            Log.e("AIViewModel", "Failed to get app-specific external storage directory.")
        } else {
            val outputDirectory = caseFile.parentFile
            if (outputDirectory != null && !outputDirectory.exists()) {
                if (!outputDirectory.mkdirs()) {
                    Log.e("AIViewModel", "Failed to create directory: ${outputDirectory.absolutePath}")
                    return
                }
            }
            try {
                FileWriter(caseFile).use { it.write(caseData.toString() + "user choice $userChoice") }
                Log.d("AIViewModel", "Saved case to ${caseFile.absolutePath}")
            } catch (e: IOException) {
                Log.e("AIViewModel", "Failed to write user choice to file", e)
            }
        }
    }
}

// You'll also need to update your GameState data class to include:
data class GameState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val casesList: List<Case> = emptyList(),
    val userChoices: Map<String, String> = emptyMap(),
    val analysisResult: String? = null,
    val gameComplete: Boolean = false,
    val currentTurn: Int = 1
)