package com.matterofchoice.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter

class AIViewModel(application: Application) : AndroidViewModel(application) {
    private var i = 0
    private val _listContent = MutableStateFlow<JSONArray?>(null)
    val listContent: StateFlow<JSONArray?> get() = _listContent

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> get() = _isInitialized

    private val _errorState = MutableStateFlow<String>("")
    val errorState: StateFlow<String> get() = _errorState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private var userGender: String = "Male"
    private var userLanguage: String = "English"
    private var userAge: String = "11"
    private var userSubject: String = "Study English"


    fun getUserInfo(gender: String, language: String, age: String, subject: String) {
        userGender = gender
        userLanguage = language
        userAge = age
        userSubject = subject

    }

    private fun loadPrompts(fileName: String): JSONObject? {
        return try {
            val jsonContent = getApplication<Application>().assets.open(fileName).bufferedReader()
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
                modelName = "gemini-pro",
                apiKey = "AIzaSyBbpQNYsB4bDDctAB14D8FQIIOqn7JOccc",
            )
            val response = model.generateContent(prompt)

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

    // Function to extract the list from the cleaned response
    fun extractList(code: String): JSONArray {
        try {
            // Parse the code assuming it's JSON-like or a list representation
            val jsonArray = JSONArray(code)

            // Convert the JSONArray to a JSON string
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
        age: Int,
        subject: String,
        prompts: JSONObject,
        context: Context,
    ) {
        Log.v("CASES", "Starting to generate cases...")


        val roles = prompts.optJSONArray("roles") ?: JSONArray()
        val casesPrompt = prompts.optString("cases")


        val role = roles.optString(i)
        val prompt =
            "$casesPrompt Respond in $language. The situations should created based on $subject and appropriate for a $sex child aged $age."

        viewModelScope.launch {
            val response = getResponseGemini(prompt)
            Log.v("COSETTE", response)
            try {
                Log.v("RAWRESPONSE", "Raw response for option $i: ${response}...")
                val cleanedResponse = cleanResponse(response)
                Log.v("cleanedResponse", "Cleaned response for option $i: ${cleanedResponse}...")
                //////////////////////////
                _listContent.value = extractList(cleanedResponse)
                _isLoading.value = false

                Log.v("SAVEDCASE", "Extracted list for option $i: ${listContent}...")

                val caseData = JSONObject()
                caseData.put("option", role)
                caseData.put("cases", JSONArray(listContent))

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

                    FileWriter(caseFile).use { it.write(caseData.toString(4)) }
                    Log.v("PATHADDRESS", "Saved case $i to ${caseFile.absolutePath}")
                }
                i++

            } catch (e: Exception) {
                Log.v("SAVEDCASE", "Error processing case $i: ${e.message}")
            }
        }
    }


    fun main() {
        val prompts = loadPrompts("prompts.json") ?: return
        _listContent.value = null
        _isInitialized.value = true

        viewModelScope.launch {
            generateCases(
                language = userLanguage,
                sex = userGender,
                age = userAge.toInt(),
                subject = userSubject,
                prompts = prompts,
                getApplication()
            )
        }
    }

}