package com.matterofchoice


import android.app.Application
import android.content.Context
import android.graphics.ImageDecoder.ImageInfo
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
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths



class ChatViewModel(application: Application) : AndroidViewModel(application){

    private var i = 0
     val listContent = MutableLiveData<JSONArray?>()


    fun loadPrompts(fileName: String): JSONObject? {
        return try {
            val jsonContent = getApplication<Application>().assets.open(fileName).bufferedReader().use { it.readText() }
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


    fun genImageCases(prompts: Map<String, Any>, logger: (String) -> Unit, getResponseGemini: (String) -> String) {
        val imageOutputPath = Paths.get("./output/images")
        Files.createDirectories(imageOutputPath)
        logger("Generating images for cases...")

        val roles = prompts["roles"] as? List<String> ?: return
        val imageTemplate = prompts["image"] as? String ?: return

        roles.forEachIndexed { index, role ->
            val prompt = imageTemplate.replace("{case}", role)
            try {
                logger("Generating image for option ${index + 1}...")
                val response = getResponseGemini(prompt)

                val url = URL(response)
                val connection = url.openConnection() as HttpURLConnection
                connection.inputStream.use { input ->
                    val image = ImageIo.read(input)
                    val imagePath = imageOutputPath.resolve("option_${index + 1}.png").toFile()
                    ImageIO.write(image, "png", imagePath)
                    logger("Image for option ${index + 1} saved at ${imagePath.absolutePath}")
                }
            } catch (e: IOException) {
                logger("Error generating image for option ${index + 1}: ${e.message}")
            } catch (e: Exception) {
                logger("Error generating image for option ${index + 1}: ${e.message}")
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
            getApplication()
        )
    }


}