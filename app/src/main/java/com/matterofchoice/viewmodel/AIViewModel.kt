package com.matterofchoice.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.matterofchoice.GameState
import com.matterofchoice.R
import com.matterofchoice.api.AnalysisRequest
import com.matterofchoice.api.AnalysisResponse
import com.matterofchoice.api.FlaskApiClient
import com.matterofchoice.model.Case
import com.matterofchoice.screens.AnalysisResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException
import com.matterofchoice.common.DropdownMapper
import com.matterofchoice.screens.PrefKeys


class AIViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences =
        application.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    private var i = 0 // Used for saving files

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> get() = _isInitialized

    private var _state = mutableStateOf(GameState())
    var state: State<GameState> = _state

    init {
        viewModelScope.launch {
            snapshotFlow { state.value.casesList }
                .collect { casesList ->
                    Log.d("AIViewModel", "Cases list updated: ${casesList?.size ?: "null"} cases")
                    // You can also log the content of the list if needed, for example:
                    // casesList?.forEachIndexed { index, case ->
                    //     Log.d("AIViewModel", "Case $index: ${case.title}")
                    // }
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
            return // Prevent multiple simultaneous calls
        }
      

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                // 1. Get user preferences from SharedPreferences
                val userSubject = sharedPreferences.getString("userSubject", null)
                val userAge = sharedPreferences.getString("userAge", null) !!
                val userGender = sharedPreferences.getString("userGender", null)
                val userLanguage = sharedPreferences.getString("userLanguage", "English")
                val subtype =  sharedPreferences.getString("subtype", null )
                val userQuestionType = sharedPreferences.getString("userQuestionType", null)
                val difficult = sharedPreferences.getString("difficult", null)

                if (userSubject == null || subtype == null || userLanguage == null || userQuestionType == null || difficult == null) {
                    _state.value = _state.value.copy(
                        error = "Please complete required fields",
                        isLoading = false
                    )
                    return@launch
                }

                // 2. Create the JSON payload for our Flask API
                val payload = JSONObject().apply {
                    put("language", userLanguage)
                    // Ensure age is an integer, provide a safe default if parsing fails
                    put("age", userAge?.toIntOrNull())
                    put("subject", userSubject)
                    put("difficulty", DropdownMapper.getDifficultyServerValue(difficult ?: "")) // TODO This can be made dynamic later
                    put("question_type", DropdownMapper.getQuestionTypeServerValue(userQuestionType ?: "")) // This can be made dynamic later
                    put("sub_type", DropdownMapper.getSubtypeServerValue(subtype ?: "")) // This can be made dynamic later
                    put("sex",  DropdownMapper.getGenderServerValue(userGender ?: ""))
                }


                val response = FlaskApiClient.startCaseGeneration(payload)// <- new direct call
                Log.d("AIViewModel", "Response from server: $response")
                _state.value = _state.value.copy(
                            isLoading = false,
                            casesList = response, // assuming API returns List<Case>
                            error = null
                )

            } catch (e: Exception) {
                Log.e("AIViewModel", "Failed to initiate game", e)
                _state.value = _state.value.copy(isLoading = false, error = "Failed to start: ${e.message}")
            }
        }
    }

    /**
     * Periodically checks the status of a background job on the server.
     * Updates the UI state based on the job's progress (pending, failed, complete).
     * @param jobId The unique identifier for the job to poll.
     */

    fun onUserChoice(caseId: String, choice: String) {
        Log.d("onUserChoice", "called with caseId: $caseId, choice: $choice, userChoices: ${_state.value.userChoices}")
        val currentChoices = _state.value.userChoices
        currentChoices[caseId] = choice
        Log.d("onUserChoice", "updated userChoices: $currentChoices")
        _state.value = _state.value.copy(userChoices = currentChoices)
    }



    fun performAnalysis(role: String) {
        Log.d("AIViewModel", "perform analysis is called")
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val cases = _state.value.casesList
                
                if (cases.isNullOrEmpty()) {
                    _state.value = _state.value.copy(isLoading = false, error = "No cases to analyze")
                    return@launch
                }

                // Ensure we have server session established (you must have called startCaseGeneration / GET /cases earlier)
//                val userId = try {
//                    FlaskApiClient.getSessionUserId()
//                } catch (e: Exception) {
//                    _state.value = _state.value.copy(isLoading = false, error = "Failed to obtain userId: ${e.message}")
//                    return@launch
//                }
               // val sharedPreferences = context.getSharedPreferences(PrefKeys.MY_PREFS, Context.MODE_PRIVATE)
                val userQuestionType = sharedPreferences.getString("userQuestionType", null)
                val userLanguage = sharedPreferences.getString("userLanguage", null)

                val request = AnalysisRequest(
//                    user_id = userId,
                    cases = cases,
                    user_choices = _state.value.userChoices,
                    role = role,
                    question_type = DropdownMapper.getQuestionTypeServerValue(userQuestionType ?: "behavioral"),
                    language = userLanguage ?: "English"
                )
                Log.d("AIViewModel154", "${request.cases.toString().substring(0..4)} ${request.user_choices.toString().substring(0..5) }")

                // 1) start analysis -> get job id
                val jobId = try {
                    FlaskApiClient.postAnalysis(request)
                } catch (e: Exception) {
                    _state.value = _state.value.copy(isLoading = false, error = "Failed to start analysis: ${e.message}")
                    Log.e("AIViewModel", "startAnalysis failed", e)
                    return@launch
                }

                Log.d("AIViewModel", "Analysis job started: $jobId. Polling...")

                // 2) Poll for status
                val pollInterval = 3000L
                val maxAttempts = 50
                var attempts = 0

                while (attempts < maxAttempts) {
                    attempts++
                    delay(pollInterval)

                    val statusResp = try {
                        FlaskApiClient.getJobStatus(jobId)
                    } catch (e: Exception) {
                        _state.value = _state.value.copy(isLoading = false, error = "Failed to poll job status: ${e.message}")
                        Log.e("AIViewModel", "poll error", e)
                        return@launch
                    }

                    when (statusResp.status.lowercase()) {
                        "pending" -> continue
                        "failed" -> {
                            val err = statusResp.error ?: "Analysis job failed on server"
                            _state.value = _state.value.copy(isLoading = false, error = err)
                            Log.e("AIViewModel", "Analysis failed: $err")
                            return@launch
                        }
                        "complete" -> {
                            // Parse the result (JsonElement) into AnalysisResponse if possible
                            val resultElem = statusResp.result
                            val finalAnalysisString = try {

                                    val jsonStr = resultElem.toString()
                                    // Try to parse to AnalysisResponse (structured)
                                    val parsed = com.google.gson.Gson().fromJson(jsonStr,
                                        AnalysisResult::class.java)

                                    _state.value = _state.value.copy(analysisResult = parsed, isLoading = false)
                                    parsed
                            } catch (e: Exception) {
                                // fallback to raw JSON string
                                statusResp.result?.toString() ?: "No analysis result"
                            }


                            Log.d("AIViewModel", "Analysis complete $finalAnalysisString")
                            return@launch
                        }
                        else -> {
                            val msg = "Unknown job status: ${statusResp.status}"
                            _state.value = _state.value.copy(isLoading = false, error = msg)
                            Log.e("AIViewModel", msg)
                            return@launch
                        }
                    }
                }

                // Timed out
                val timeoutMsg = "Analysis timed out after ${(pollInterval * maxAttempts) / 1000} seconds."
                _state.value = _state.value.copy(isLoading = false, error = timeoutMsg)
                Log.e("AIViewModel", timeoutMsg)

            } catch (e: Exception) {
                Log.e("AIViewModel", "performAnalysis failed", e)
                _state.value = _state.value.copy(error = e.message, isLoading = false)
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
