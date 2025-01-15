package com.matterofchoice.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.matterofchoice.model.MessageModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException

class AIViewModel(application: Application) : AndroidViewModel(application) {

    private val AiHistory by lazy {
        mutableListOf<MessageModel>()
    }


    private var _analysisChoices = MutableStateFlow<String>("")
    var analysisChoices: StateFlow<String> = _analysisChoices

    private val sharedPreferences =
        application.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    private var i = 0
    private val _listContent = MutableStateFlow<List<JSONArray?>>(emptyList())
    val listContent: StateFlow<List<JSONArray?>> get() = _listContent

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> get() = _isInitialized

    private val _errorState = MutableStateFlow<String>("")
    val errorState: StateFlow<String> get() = _errorState

    private val _errorAnalysis = MutableStateFlow<String>("")
    val errorAnalysis: StateFlow<String> get() = _errorState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _isLoadingAnalysis = MutableStateFlow(false)
    val isLoadingAnalysis: StateFlow<Boolean> get() = _isLoadingAnalysis

    private var userSubject = sharedPreferences.getString("userSubject", "")
    private var userAge = sharedPreferences.getString("userAge", "")
    private val userGender = sharedPreferences.getString("userGender", "")
    private var userLanguage = sharedPreferences.getString("userLanguage", "")

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
            _isLoading.value = true
            Log.v("TOOLRES", "Generating response for prompt: $prompt...")

            val model = GenerativeModel(
                modelName = "gemini-2.0-flash-exp",
                apiKey = "AIzaSyBbpQNYsB4bDDctAB14D8FQIIOqn7JOccc",
            )
            val gemini = model.startChat(
                history = AiHistory.map {
                    content(it.role){
                        text(it.message)
                    }
                }.toList()
            )
            val response = gemini.sendMessage(prompt)
            AiHistory.add(MessageModel(prompt,"user"))
            AiHistory.add(MessageModel(response.text.toString(),"model"))


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
            _errorState.value = err.message!!
            ""
        } finally {
            _isLoading.value = false
        }
    }


    fun cleanResponse(response: String): String {
        val scenariosRegex = Regex("""^\s*cases\s*=\s*""")
        val codeBlockRegex = Regex("""^```[a-zA-Z]*\n|```$""")

        return response
            .replace(codeBlockRegex, "")
            .replace(scenariosRegex, "")
            .trim()
    }

    fun extractList(code: String): JSONArray {
        try {

            val jsonArray = JSONArray(code)


            return jsonArray
        } catch (e: Exception) {
            // Log the error and problematic code
            Log.v("JSONCONVERT", "ExtractList Error parsing or evaluating code: ${e.message}")
            Log.v("JSONCONVERT", "Problematic code snippet: $code")
            return JSONArray("")
        }
    }

    // Function to generate cases for different scenarios
    fun generateCases(
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

                _listContent.value += extractList(cleanedResponse)
                _isLoading.value = false
                Log.v("SAVEDCASE", "Extracted list for option $i: ${listContent}...")


            } catch (e: Exception) {
                Log.v("SAVEDCASE", "Error processing case $i: ${e.message}")
            }
        }
    }

    fun saveUserChoice(context: Context, caseDate: JSONArray, userChoice: String) {
//        val caseData = JSONObject()
//        caseData.put("option", role)
//        caseData.put("cases", JSONArray(listContent))

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
            FileWriter(caseFile).use { it.write(caseDate.toString(4) + "user choice $userChoice") }
            Log.v("PATHADDRESS", "Saved case $i to ${caseFile.absolutePath}")
        }
    }


    fun main() {
        val prompts = loadPrompts() ?: return
        _listContent.value = emptyList()
        _isInitialized.value = true

        viewModelScope.launch {
            for (i in 0 until 2){
                generateCases(
                    language = userLanguage!!,
                    sex = userGender!!,
                    age = userAge!!,
                    subject = userSubject!!,
                    prompts = prompts,
                )
            }

        }
    }

    fun loadAnalysis(context: Context): String {
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

        val model = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = "AIzaSyBbpQNYsB4bDDctAB14D8FQIIOqn7JOccc",
        )
        viewModelScope.launch {
            try {
                val analysis =
                    model.generateContent("This is each scenario with the user choice. provide me with analysis and recommendations of the user: $userChoices")
                _analysisChoices.value = analysis.text!!
            } catch (e: Exception) {
                _errorAnalysis.value = e.message.toString()
                _isLoadingAnalysis.value = false
            }
        }
        return userChoices
    }

}