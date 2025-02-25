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

class AIViewModel(application: Application) : AndroidViewModel(application) {

    private val model = GenerativeModel(
        modelName = "gemini-2.0-flash-exp",
        apiKey = "AIzaSyBbpQNYsB4bDDctAB14D8FQIIOqn7JOccc",
    )

    private val aiHistory by lazy {
        mutableListOf<MessageModel>()
    }


    private var _analysisChoices = MutableStateFlow("")
    var analysisChoices: StateFlow<String> = _analysisChoices

    private val sharedPreferences =
        application.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    private var i = 0

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> get() = _isInitialized

    private val _errorAnalysis = MutableStateFlow("")
    val errorAnalysis: StateFlow<String> get() = _errorAnalysis


    private val _isLoadingAnalysis = MutableStateFlow(false)
    val isLoadingAnalysis: StateFlow<Boolean> get() = _isLoadingAnalysis

    var _state = mutableStateOf(GameState())
    var state: State<GameState> = _state

    private var userSubject = sharedPreferences.getString("userSubject", "random subject")
    private var userAge = sharedPreferences.getString("userAge", "any")
    private val userGender = sharedPreferences.getString("userGender", "any")
    private var userLanguage = sharedPreferences.getString("userLanguage", "English")
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
        val apiKey = "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"

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

    fun resetAnalysisState() {
        _analysisChoices.value = ""
        _errorAnalysis.value = ""
        _isLoadingAnalysis.value = false
    }

    fun loadAnalysis(context: Context, role: String): String {
        val outputPath = context.getExternalFilesDir("tool")?.absolutePath ?: ""
        var userChoices = ""

        _isLoadingAnalysis.value = true
        if (outputPath.isEmpty()) {
            Log.e("OUTPUT_PATH", "Failed to get app-specific external storage directory.")
            _errorAnalysis.value = "No data to analysis"
            _isLoadingAnalysis.value = false
        } else {
            val outputDirectory = File(outputPath)

            if (outputDirectory.exists() && outputDirectory.isDirectory) {
                val files = outputDirectory.listFiles() // Get all files in the directory
                if (files != null && files.isNotEmpty()) {
                    for (file in files) {
                        if (file.isFile) { // Check if it's a file
                            try {
                                val fileContents = file.readText()
                                userChoices += fileContents
                                Log.v("FILE_CONTENTS", "Contents of ${file.name}: $fileContents")
                            } catch (e: IOException) {
                                Log.e("FILE_READ", "Failed to read file ${file.name}: ${e.message}")

                                _errorAnalysis.value = "No data to analysis"
                                _isLoadingAnalysis.value = false
                            }
                        }
                    }
                } else {
                    Log.v("DIRECTORY", "No files found in the directory.")
                    _errorAnalysis.value = "No data to analysis"
                    _isLoadingAnalysis.value = false
                }
            } else {
                Log.e("DIRECTORY", "Output directory does not exist or is not a directory.")
                _errorAnalysis.value = "No data to analysis"
                _isLoadingAnalysis.value = false
            }
        }

        viewModelScope.launch {
            try {
                val analysis =
                    model.generateContent("This is each scenario with the user choice and the role is $role. provide me with concise and general analysis and recommendations of the user: $userChoices")
                _analysisChoices.value = analysis.text!!
                _isLoadingAnalysis.value = false
            } catch (e: Exception) {
                _errorAnalysis.value = e.message.toString()
                _isLoadingAnalysis.value = false
            }
        }
        return userChoices
    }

}