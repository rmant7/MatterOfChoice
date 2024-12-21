package com.matterofchoice

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter


class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private var i = 0
    val listContent = MutableLiveData<JSONArray?>()

    private lateinit var userGender:String
    private lateinit var userLanguage: String
    private lateinit var userAge:String
    private lateinit var userSubject: String


    fun getUserInfo(gender:String,language:String, age:String, subject:String){
        userGender = gender
        userLanguage = language
        userAge = age
        userSubject = subject

    }


    fun loadPrompts(fileName: String): JSONObject? {
        return try {
            val jsonContent = getApplication<Application>().assets.open(fileName).bufferedReader()
                .use { it.readText() }
            JSONObject(jsonContent)
        } catch (e: Exception) {
            Log.v("TAGY", "Error loading prompts: ${e.message}")
            null
        }
    }

    suspend fun getResponseGemini(prompt: String): String {
        return try {
            Log.v("TOOLRES", "Generating response for prompt: $prompt...")

            val model = GenerativeModel(
                modelName = "gemini-pro",
                apiKey = Constants.apiKey,
            )
            val response = model.generateContent(prompt)

            if (response != null && response.candidates.isNotEmpty()) {
                val content = response.text!!
                Log.v("MOSAA", "Received response: $content")
                content
            } else {
                Log.v("CONTENT", "Empty or invalid response from Gemini.")
                ""
            }
        } catch (err: Exception) {
            Log.v("CONTENT", "Error generating response: ${err.message}")
            ""
        }
    }


    fun cleanResponse(response: String): String {
        val scenariosRegex = Regex("""^\s*cases\s*=\s*""")
        val codeBlockRegex = Regex("""^```[a-zA-Z]*\n|```$""")

        return response
            .replace(codeBlockRegex, "") // Remove markdown code block markers
            .replace(scenariosRegex, "") // Remove the `scenarios =` prefix
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
        context: Context
    ) {
        Log.v("CASES", "Starting to generate cases...")


        val roles = prompts.optJSONArray("roles") ?: JSONArray()
        val casesPrompt = prompts.optString("cases")


        val role = roles.optString(i)
        val prompt =
            "$casesPrompt Respond in $language. The content should be appropriate for a $sex child aged $age and the subject is $subject"

        viewModelScope.launch {
            val response = getResponseGemini(prompt)
            Log.v("COSETTE", response)
            try {
                Log.v("RAWRESPONSE", "Raw response for option $i: ${response}...")
                val cleanedResponse = cleanResponse(response)
                Log.v("cleanedResponse", "Cleaned response for option $i: ${cleanedResponse}...")
                //////////////////////////
                listContent.postValue(extractList(cleanedResponse))

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
        listContent.value = null
        if (userLanguage != null){
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