package com.matterofchoice.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
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

class AIViewModel(application: Application): AndroidViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    private val _userSubject = MutableStateFlow(sharedPreferences.getString("username", "") ?: "")
    private val userSubject: StateFlow<String> get() = _userSubject

    private val _userAge = MutableStateFlow(sharedPreferences.getString("username", "") ?: "")
    private val userAge: StateFlow<String> get() = _userAge

    private val _userGender = MutableStateFlow(sharedPreferences.getString("username", "") ?: "")
    private val userGender: StateFlow<String> get() = _userGender

    private val _userLanguage = MutableStateFlow(sharedPreferences.getString("username", "") ?: "")
    private val userLanguage: StateFlow<String> get() = _userLanguage

    private val _isInitialized = mutableStateOf(false)
    val isInitialized: State<Boolean> = _isInitialized



    private val _listContent = MutableStateFlow<JSONArray?>(null)
    var listContent: StateFlow<JSONArray?> = _listContent

    private val _errorState = MutableStateFlow<String?>(null)
    var errorState: StateFlow<String?> = _errorState


    private var caseIndex = 0


    private fun loadPrompts(): JSONObject? {
        return try {
            val jsonContent = getApplication<Application>().assets.open("prompts.json").bufferedReader().use { it.readText() }
            JSONObject(jsonContent)
        } catch (e: Exception) {
            Log.v("TAGY", "Error loading prompts: ${e.message}")
            null
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
        return try {
            JSONArray(code)
        } catch (e: Exception) {
            Log.v("JSONCONVERT", "Error parsing or evaluating code: ${e.message}")
            Log.v("JSONCONVERT", "Problematic code snippet: $code")
            JSONArray("[]")
        }
    }

    fun initializeCases(context: Context) {
        if (!_isInitialized.value) {
            _isInitialized.value = true
            val result = loadPrompts()
            if (result != null) {
                generateCases(result, context)
            } else {
                _errorState.value = "Failed to load prompts."
            }

        }
    }

    private fun saveGeneratedCase(i: Int, list: JSONArray, role: String, context: Context) {
        try {
            val caseData = JSONObject().apply {
                put("option", role)
                put("cases", list)
            }

            val outputPath = context.getExternalFilesDir("tool")?.absolutePath ?: ""
            if (outputPath.isEmpty()) {
                Log.e("OUTPUT_PATH", "Failed to get app-specific external storage directory.")
                return
            }

            val caseFile = File(outputPath, "option_$i.json")
            val parentDir = caseFile.parentFile
            if (parentDir != null && !parentDir.exists() && parentDir.mkdirs()) {
                Log.v("DIRECTORY", "Created directory: ${parentDir.absolutePath}")
            }

            FileWriter(caseFile).use { it.write(caseData.toString(4)) }
            Log.v("PATHADDRESS", "Saved case $i to ${caseFile.absolutePath}")
        } catch (e: Exception) {
            Log.v("Ffsf", e.message.toString())
        }
    }

    private suspend fun generateCasePrompt(i: Int, prompts: JSONObject, context: Context) {
        val roles = prompts.optJSONArray("roles") ?: JSONArray()
        val role = roles.optString(i)
        val casesPrompt = prompts.optString("cases")

        val prompt = "$casesPrompt Respond in $userLanguage. The situation should be created based on $userSubject and appropriate for a $userGender child aged $userAge."

        val response = getResponseGemini(prompt)
        val cleanedResponse = cleanResponse(response)
        val extractedList = extractList(cleanedResponse)
        _listContent.value = extractedList

        saveGeneratedCase(i, extractedList, role, context)
    }

    private fun generateCases(prompts: JSONObject, context: Context) {
        viewModelScope.launch {
            try {
                generateCasePrompt(caseIndex, prompts, context)
                caseIndex++
            } catch (e: Exception) {
                Log.v("CASES", "Error processing case $caseIndex: ${e.message}")
                _errorState.value = "Error processing case: ${e.message}"
            }
        }

    }

    private suspend fun getResponseGemini(prompt: String): String {
        return try {
            Log.v("TOOLRES", "Generating response for prompt: $prompt...")

            val model = GenerativeModel(
                modelName = "gemini-pro",
                apiKey = "AIzaSyBbpQNYsB4bDDctAB14D8FQIIOqn7JOccc",
            )
            val response = model.generateContent(prompt)

            if (response.candidates.isNotEmpty()) {
                val content = response.text ?: ""
                Log.v("MOSAA", "Received response: $content")
                content
            } else {
                Log.v("CONTENT", "Empty or invalid response from Gemini.")
                ""
            }
        } catch (err: Exception) {
            Log.v("AHMEDCONTENT", "Error generating response: ${err.message}")
            _errorState.value = "Error generating response: ${err.message}"
            ""
        }
    }

}