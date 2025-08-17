package com.matterofchoice.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.matterofchoice.GameState
import com.matterofchoice.R
import com.matterofchoice.api.AnalysisRequest
import com.matterofchoice.api.FlaskApiClient
import com.matterofchoice.model.Case
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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


class AIViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences =
        application.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    private var i = 0 // Used for saving files

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> get() = _isInitialized

    private var _state = mutableStateOf(GameState())
    var state: State<GameState> = _state

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
            _state.value = _state.value.copy(isLoading = true, error = null, casesList = emptyList())
            try {
                // 1. Get user preferences from SharedPreferences
                val userSubject = sharedPreferences.getString("userSubject", "life skills")!!
                val userAge = sharedPreferences.getString("userAge", "25")!!
                val userGender = sharedPreferences.getString("userGender", "any")!!
                val userLanguage = sharedPreferences.getString("userLanguage", "English")!!

                // 2. Create the JSON payload for our Flask API
                val payload = JSONObject().apply {
                    put("language", userLanguage)
                    // Ensure age is an integer, provide a safe default if parsing fails
                    put("age", userAge.toIntOrNull() ?: 25)
                    put("subject", userSubject)
                    put("difficulty", "medium") // TODO This can be made dynamic later
                    put("question_type", "behavioral") // This can be made dynamic later
                    put("sub_type", "scenario_analysis") // This can be made dynamic later
                    put("sex", userGender)
                }

                // 3. Start the job on the backend and get a job ID
                Log.d("AIViewModel", "Starting case generation job...")
                val jobId = FlaskApiClient.startCaseGeneration(payload)
                Log.d("AIViewModel", "Job started with ID: $jobId. Now polling for results...")

                // 4. Poll for the result in a non-blocking way
                pollForJobResult(jobId)

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
    private suspend fun pollForJobResult(jobId: String) {
        val maxPollTime = 150_000L // 2.5 minutes total timeout
        val pollInterval = 5_000L  // Check status every 5 seconds
        var elapsedTime = 0L

        while (elapsedTime < maxPollTime) {
            try {
                val statusResponse = FlaskApiClient.getJobStatus(jobId)
                Log.d("AIViewModel", "Job status: ${statusResponse.status}")

                when (statusResponse.status) {
                    "complete" -> {
                        val cases = statusResponse.result ?: emptyList()
                        _state.value = _state.value.copy(
                            isLoading = false,
                            casesList = cases,
                            error = null
                        )
                        Log.i("AIViewModel", "Job complete! Received ${cases.size} cases.")
                        return // Success! Exit the loop and function.
                    }
                    "failed" -> {
                        val errorMessage = "Generation failed on server: ${statusResponse.error}"
                        _state.value = _state.value.copy(isLoading = false, error = errorMessage)
                        Log.e("AIViewModel", errorMessage)
                        return // Failure. Exit the loop and function.
                    }
                    "pending" -> {
                        // Job is not done yet. Wait for the interval and then poll again.
                        delay(pollInterval)
                        elapsedTime += pollInterval
                    }
                    else -> {
                        // Unexpected status from the server
                        val unexpectedStatusMessage = "Received unknown status from server: ${statusResponse.status}"
                        _state.value = _state.value.copy(isLoading = false, error = unexpectedStatusMessage)
                        Log.e("AIViewModel", unexpectedStatusMessage)
                        return
                    }
                }
            } catch (e: Exception) {
                val errorMessage = "Error while polling for result: ${e.message}"
                _state.value = _state.value.copy(isLoading = false, error = errorMessage)
                Log.e("AIViewModel", errorMessage, e)
                return // Exit on a network or parsing error during polling.
            }
        }

        // If the while loop finishes without returning, it means we timed out.
        val timeoutMessage = "Request timed out after ${maxPollTime / 1000} seconds."
        _state.value = _state.value.copy(isLoading = false, error = timeoutMessage)
        Log.e("AIViewModel", timeoutMessage)
    }

    fun onUserChoice(caseId: String, choice: String) {
        val currentChoices = _state.value.userChoices
        currentChoices[caseId] = choice
        _state.value = _state.value.copy(userChoices = currentChoices)
    }

    fun performAnalysis() {
        Log.d("AIViewModel", "perform analysis is called")
        viewModelScope.launch {
            Log.d("AIViewModel", "perform analysis is called within viewmodel")
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                Log.d("AIViewModel", "perform analysis is called within try")
                Log.d("AiViewModel", _state.value.toString())
                val request = AnalysisRequest(
                    cases = _state.value.casesList!!,
                    user_choices = _state.value.userChoices,
                    role = "Parent",
                    question_type = "behavioral",
                    language = "English"
                )
                Log.d("AIViewModel", "making analysis request")
                val response = FlaskApiClient.postAnalysis(request)
                _state.value = _state.value.copy(analysisResult = response.analysis, isLoading = false)
                Log.d("info", response.analysis)
            } catch (e: Exception) {
                Log.d("AIViewModel", e.toString())
                _state.value = _state.value.copy(error = e.message, isLoading = false)
                e.message?.let { Log.d("info", it) }

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

    /**
     * Generates an image using a third-party service (Hugging Face).
     * This can be moved to the backend in the future to hide the API key,
     * but for now, it remains on the client.
     */
    // fun generateImage(prompt: String, context: Context) {
    //     // Show a placeholder immediately for a better user experience
    //     val placeholderBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.place_holder)
    //     _state.value = _state.value.copy(image = placeholderBitmap)
    //
    //     val client = OkHttpClient()
    //
    //     // WARNING: Storing API keys in client-side code is not secure for production.
    //     // This should be moved to a secure backend or BuildConfig fields for a real app.
    //     val apiKey = "your_hugging_face_api_key" // TODO: Secure this key
    //
    //     val json = JSONObject()
    //     json.put("inputs", prompt)
    //     json.put("parameters", JSONObject().put("num_inference_steps", 5))
    //
    //     val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
    //
    //     val request = Request.Builder()
    //         .url("https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-3.5-large-turbo")
    //         .addHeader("Authorization", "Bearer $apiKey")
    //         .addHeader("Content-Type", "application/json")
    //         .post(requestBody)
    //         .build()
    //
    //     client.newCall(request).enqueue(object : Callback {
    //         override fun onFailure(call: Call, e: IOException) {
    //             Log.e("AIViewModel", "Image generation request failed", e)
    //             // Optionally update UI to show an error state for the image
    //         }
    //
    //         override fun onResponse(call: Call, response: Response) {
    //             response.use {
    //                 if (!response.isSuccessful) {
    //                     Log.e("AIViewModel", "Image generation failed with code: ${response.code}")
    //                     return
    //                 }
    //
    //                 val imageBytes = response.body?.bytes()
    //                 imageBytes?.let {
    //                     val generatedBitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
    //                     _state.value = _state.value.copy(image = generatedBitmap)
    //                 }
    //             }
    //         }
    //     })
    // }
}
