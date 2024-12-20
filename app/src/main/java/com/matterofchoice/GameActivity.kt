package com.matterofchoice

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.matterofchoice.model.Case
import com.matterofchoices.R
import com.matterofchoices.databinding.ActivityGameBinding
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter


class GameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameBinding
    private var i = 0

    private var listContent: JSONArray? = JSONArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        main()
        binding.button3.setOnClickListener {
            try {
                val gson = Gson()
                val listType = object : TypeToken<List<Case>>() {}.type
                val cases: List<Case> = gson.fromJson(listContent.toString(), listType)

                Log.v("KARSHA",cases.toString())

                binding.apply {

                    optionTxt.text = cases[0].case

                    option1TV.text = cases[0].options[0].option
                    option2TV.text = cases[0].options[1].option
                    option3TV.text = cases[0].options[2].option
                    option4TV.text = cases[0].options[3].option

                }


            } catch (e: Exception) {
                Log.v("DISPLAYING", e.message.toString())
            }
        }
    }


    fun loadPrompts(fileName: String): JSONObject? {
        return try {
            val jsonContent = assets.open(fileName).bufferedReader().use { it.readText() }
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
        prompts: JSONObject,
        context: Context
    ) {
        Log.v("CASES", "Starting to generate cases...")


        val roles = prompts.optJSONArray("roles") ?: JSONArray()
        val casesPrompt = prompts.optString("cases")


        val role = roles.optString(i)
        val prompt =
            "$casesPrompt Respond in $language. The content should be appropriate for a $sex child aged $age"

        lifecycleScope.launch {
            val response = getResponseGemini(prompt)
            Log.v("COSETTE", response)
            try {
                Log.v("RAWRESPONSE", "Raw response for option $i: ${response}...")
                val cleanedResponse = cleanResponse(response)
                Log.v("cleanedResponse", "Cleaned response for option $i: ${cleanedResponse}...")
                //////////////////////////
                listContent = extractList(cleanedResponse)
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
        generateCases(
            language = "English",
            sex = "male",
            age = 8,
            prompts = prompts,
            applicationContext
        )
    }

}