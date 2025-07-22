package com.matterofchoice.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.matterofchoice.GameState
import com.matterofchoice.R
import com.matterofchoice.model.Case
import com.matterofchoice.model.MessageModel
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException
import com.matterofchoice.api.FlaskApiClient // <-- IMPORT your new client
import kotlinx.coroutines.delay

class AI_ViewModel(application: Application) : AndroidViewModel(application) {

    private val model = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = "AIzaSyD0Fy2Pm_vvhX3cbm4zjmTKnc6VPMkJ_VA",
    )

    private val aiHistory by lazy {
        mutableListOf<MessageModel>()
    }


    private val sharedPreferences =
        application.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    private var i = 0

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> get() = _isInitialized

    var _state = mutableStateOf(GameState())
    var state: State<GameState> = _state


    private val cases = mutableListOf<Case>()

    private fun loadPrompts(): JSONObject? {

        return try {
            val jsonContent =
                getApplication<Application>().assets.open("prompts.json").bufferedReader()
                    .use { it.readText() }
            JSONObject(jsonContent)
        } catch (e: Exception) {
            Log.v("TAGY", "Error loading prompts: ${e.message}")
            null
        }
    }

    private suspend fun getResponseGemini(prompt: String): String {
        return try {
            _state.value = _state.value.copy(isLoading = true)
            Log.v("TOOLRES", "Generating response for prompt: $prompt...")

            val gemini = model.startChat(
                history = aiHistory.map {
                    content(it.role) {
                        text(it.message)
                    }
                }.toList()
            )
            val response = gemini.sendMessage(prompt)
            aiHistory.add(MessageModel(prompt, "user"))
            aiHistory.add(MessageModel(response.text.toString(), "model"))


            if (response.candidates.isNotEmpty()) {
                val content = response.text!!
                Log.v("MOSAA", "Received response: $content")
                content
            } else {
                Log.v("CONTENT", "Empty or invalid response from Gemini.")
                ""
            }
        } catch (err: Exception) {
            Log.v("AHMEDCONTENT", "Error generating response: ${err.message}")
            _state.value = _state.value.copy(error = err.message)
            ""
        } finally {
            _state.value = _state.value.copy(isLoading = false)
        }
    }


    private fun cleanResponse(response: String): String {
        val scenariosRegex = Regex("""^\s*cases\s*=\s*""")
        val codeBlockRegex = Regex("""^```[a-zA-Z]*\n|```$""")

        return response
            .replace(codeBlockRegex, "")
            .replace(scenariosRegex, "")
            .trim()
    }

    private fun extractList(code: String): JSONArray {
        try {

            val jsonArray = JSONArray(code)


            return jsonArray
        } catch (e: Exception) {
            Log.v("JSONCONVERT", "ExtractList Error parsing or evaluating code: ${e.message}")
            Log.v("JSONCONVERT", "Problematic code snippet: $code")
            return JSONArray("")
        }
    }

    private fun generateCases(
        language: String,
        sex: String,
        age: String,
        subject: String,
        prompts: JSONObject,
    ) {
        Log.v("CASES", "Starting to generate cases...")


        val casesPrompt = prompts.optString("cases")


        val prompt =
            "$casesPrompt Respond in $language. The situations should created based on $subject and appropriate for a $sex child aged $age."

        viewModelScope.launch {
            val response = getResponseGemini(prompt)
            Log.v("COSETTE", response)
            try {
                Log.v("RAWRESPONSE", "Raw response for option $i: ${response}...")
                val cleanedResponse = cleanResponse(response)
                Log.v("cleanedResponse", "Cleaned response for option $i: ${cleanedResponse}...")

                val gson = Gson()
                val listType = object : TypeToken<Case>() {}.type
                val lists: JSONArray = extractList(cleanedResponse)


                for (i in 0 until lists.length()) {
                    val jsonObject = lists.getJSONObject(i) // Get each JSONObject in the JSONArray
                    val caseList: Case = gson.fromJson(
                        jsonObject.toString(),
                        listType
                    ) // Deserialize into List<Case>
                    cases.add(caseList) // Add all cases to the mutable list
                }

                _state.value = _state.value.copy(casesList = cases)
                _state.value = _state.value.copy(isLoading = false)

            } catch (e: Exception) {
                Log.v("SAVEDCASE", "Error processing case $i: ${e.message}")
            }
        }
    }

    fun saveUserChoice(context: Context, caseDate: Case, userChoice: String) {
        val outputPath = context.getExternalFilesDir("tool")?.absolutePath ?: ""
        val caseFile = File(outputPath, "option_$i.json")


        if (outputPath.isEmpty()) {
            Log.e("OUTPUT_PATH", "Failed to get app-specific external storage directory.")
        } else {
            val outputDirectory = caseFile.parentFile
            if (outputDirectory != null && !outputDirectory.exists()) {
                if (outputDirectory.mkdirs()) {
                    Log.v("DIRECTORY", "Created directory: ${outputDirectory.absolutePath}")
                } else {
                    Log.e(
                        "DIRECTORY",
                        "Failed to create directory: ${outputDirectory.absolutePath}"
                    )
                }
            }
            FileWriter(caseFile).use { it.write(caseDate.toString() + "user choice $userChoice") }
            Log.v("PATHADDRESS", "Saved case $i to ${caseFile.absolutePath}")
        }
    }


    fun generateImage(prompt: String,context: Context) {
        val bitmap = BitmapFactory.decodeResource(context.resources,R.drawable.place_holder)
        _state.value = _state.value.copy(image = bitmap)
        val client = OkHttpClient()

        // API Key (Keep this secure, don't hardcode it in production)
        val apiKey = "your_api_key"

        // JSON request body
        val json = JSONObject()
        json.put("inputs", prompt)
        json.put("parameters", JSONObject().put("num_inference_steps", 5))

        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())

        val request = Request.Builder()
            .url("https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-3.5-large-turbo")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                println("Request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        println("Request failed with code: ${response.code}")
                        return
                    }
                    val responseBody = response.body

                    val fetchImage = responseBody?.bytes()

                    fetchImage?.let {
                        val imageCreated = BitmapFactory.decodeByteArray(fetchImage, 0, fetchImage.size)
                        _state.value = _state.value.copy(image = imageCreated)
                    }



                }
            }
        })
    }

    fun main() {
        Log.v("ErrorGEMINI", "view model main function called")

         val userSubject = sharedPreferences.getString("userSubject", "random subject")
         val userAge = sharedPreferences.getString("userAge", "any")
         val userGender = sharedPreferences.getString("userGender", "any")
         val userLanguage = sharedPreferences.getString("userLanguage", "English")



        val delayTime = 4000L
        val prompts = loadPrompts() ?: return
        _isInitialized.value = true

        viewModelScope.launch {
            for (i in 0 until 4) {
                generateCases(
                    language = userLanguage!!,
                    sex = userGender!!,
                    age = userAge!!,
                    subject = userSubject!!,
                    prompts = prompts,
                )
                delay(delayTime)
                delayTime * 2
            }

        }
    }



}


class AIViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences =
        application.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    private var i = 0 // Used for saving files

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> get() = _isInitialized

    var _state = mutableStateOf(GameState())
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
                    put("difficulty", "medium") // This can be made dynamic later
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
    fun generateImage(prompt: String, context: Context) {
        // Show a placeholder immediately for a better user experience
        val placeholderBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.place_holder)
        _state.value = _state.value.copy(image = placeholderBitmap)

        val client = OkHttpClient()

        // WARNING: Storing API keys in client-side code is not secure for production.
        // This should be moved to a secure backend or BuildConfig fields for a real app.
        val apiKey = "your_hugging_face_api_key" // TODO: Secure this key

        val json = JSONObject()
        json.put("inputs", prompt)
        json.put("parameters", JSONObject().put("num_inference_steps", 5))

        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())

        val request = Request.Builder()
            .url("https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-3.5-large-turbo")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AIViewModel", "Image generation request failed", e)
                // Optionally update UI to show an error state for the image
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("AIViewModel", "Image generation failed with code: ${response.code}")
                        return
                    }

                    val imageBytes = response.body?.bytes()
                    imageBytes?.let {
                        val generatedBitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                        _state.value = _state.value.copy(image = generatedBitmap)
                    }
                }
            }
        })
    }
}