package com.matterofchoice

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
//class ExampleUnitTest {
//    @Test
//    fun addition_isCorrect() {
//        assertEquals(4, 2 + 2)
//    }
//}

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class OkHttpFlaskClient() {
    private val baseUrl: String = "https://congenial-palm-tree-57qvwq7gjp9h4xrp-5000.app.github.dev"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // 1. POST /generate_cases
    fun generateCases(json: JSONObject, sessionCookie: String? = null): Response? {
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$baseUrl/generate_cases")
            .post(body)
            .apply { sessionCookie?.let { addHeader("Cookie", it) } }
            .build()
        return client.newCall(request).execute()
    }

    // 2. GET /get_image/<case_id>
    fun getImage(caseId: String, sessionCookie: String? = null): Response? {
        val request = Request.Builder()
            .url("$baseUrl/get_image/$caseId")
            .get()
            .apply { sessionCookie?.let { addHeader("Cookie", it) } }
            .build()
        return client.newCall(request).execute()
    }

    // 3. POST /reset
    fun reset(sessionCookie: String? = null): Response? {
        val request = Request.Builder()
            .url("$baseUrl/reset")
            .post("".toRequestBody()) // Empty body
            .apply { sessionCookie?.let { addHeader("Cookie", it) } }
            .build()
        return client.newCall(request).execute()
    }

    // 4. POST /converse
    fun converse(text: String, sessionCookie: String? = null): Response? {
        val json = JSONObject().put("text", text)
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$baseUrl/converse")
            .post(body)
            .apply { sessionCookie?.let { addHeader("Cookie", it) } }
            .build()
        return client.newCall(request).execute()
    }

    // 5. POST /analyze-image
    fun analyzeImage(imageFile: File, prompt: String, sessionCookie: String? = null): Response? {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", imageFile.name, imageFile.asRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("prompt", prompt)
            .build()
        val request = Request.Builder()
            .url("$baseUrl/analyze-image")
            .post(requestBody)
            .apply { sessionCookie?.let { addHeader("Cookie", it) } }
            .build()
        return client.newCall(request).execute()
    }

    // 6. POST /analysis
    fun analysis(json: JSONObject, sessionCookie: String? = null): Response? {
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$baseUrl/analysis")
            .post(body)
            .apply { sessionCookie?.let { addHeader("Cookie", it) } }
            .build()
        return client.newCall(request).execute()
    }

    // 7. POST /submit_responses
    fun submitResponses(json: JSONObject, sessionCookie: String? = null): Response? {
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$baseUrl/submit_responses")
            .post(body)
            .apply { sessionCookie?.let { addHeader("Cookie", it) } }
            .build()
        return client.newCall(request).execute()
    }
    @Test
    fun test(){
        val client = OkHttpFlaskClient()
        val response = client.generateCases(JSONObject())
        println(response?.body?.string())
    }
}