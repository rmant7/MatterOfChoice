package com.matterofchoice.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.matterofchoice.screens.Case
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Data classes for the new API responses
data class GenerateCasesResponse(val data: List<Case>)

data class AnalysisResultResponse(
    val overall_judgement: String,
    val cases: List<CaseAnalysis>
)

data class CaseAnalysis(
    val case_description: String,
    val player_choice: String,
    val optimal_choice: String,
    val analysis: String
)

data class StartAnalysisResponse(val job_id: String)
data class AnalysisStatusResponse(
    val status: String,
    val result: AnalysisResultResponse?,
    val error: String?
)

/**
 * API client for the Modal-deployed backend
 */
object ModalApiClient {

    private const val BASE_URL = "https://reqeique--matter-of-choice-fastapi-app.modal.run"
    private var currentUserId: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(160, TimeUnit.SECONDS)
        .readTimeout(160, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            private val cookieStore = mutableMapOf<String, List<Cookie>>()

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                Log.d("ModalApiClient", "Saving cookies: $cookies")
                cookieStore[url.host] = cookies
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val cookies = cookieStore[url.host] ?: emptyList()
                Log.d("ModalApiClient", "Loading cookies for ${url.host}: $cookies")
                return cookies
            }
        })
        .addInterceptor { chain ->
            val request = chain.request()
            Log.d("ModalApiClient", "Outgoing request to: ${request.url}")
            Log.d("ModalApiClient", "Request headers: ${request.headers}")
            val response = chain.proceed(request)
            Log.d("ModalApiClient", "Response cookies: ${response.headers("Set-Cookie")}")
            response
        }
        .build()

    private val gson = Gson()

    /**
     * Initialize user session by visiting /cases endpoint
     */
    suspend fun initializeSession(): Boolean = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url("$BASE_URL/cases")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { it ->

                    // Check if session cookie is set
                    val cookies = it.headers("Set-Cookie")
                    val hasSessionCookie = cookies.any { it.contains("session") }
                    val sessionCookie = cookies.find { it.contains("session") }
                    Log.d("ModalApiClient", "Session initialization - Cookies: $cookies")
                    Log.d("ModalApiClient", "Has session cookie: $hasSessionCookie")

                    // Parse user ID from session cookie (this might need adjustment)
                    currentUserId = extractUserIdFromCookie(sessionCookie)

                    continuation.resume(it.isSuccessful && hasSessionCookie)

                }
            }
        })
    }

    /**
     * Generate cases with user parameters
     */
    suspend fun generateCases(
        language: String,
        subject: String,
        difficulty: String,
        questionType: String,
        subType: String,
        age: Int? = null,
        sex: String? = null,
        role: String? = null,
        answers: Map<String, String>? = null
    ): List<Case> = suspendCancellableCoroutine { continuation ->

        val payload = JSONObject().apply {
            put("user_id", currentUserId ?: "unknown")
            put("language", language)
            put("subject", subject)
            put("difficulty", difficulty)
            put("question_type", questionType)
            put("sub_type", subType)
            age?.let { put("age", it) }
            sex?.let { put("sex", it) }
            role?.let { put("role", it) }
            answers?.let { put("answers", JSONObject(it)) }
        }
        Log.d("ModalApiClient", "=== GENERATE CASES REQUEST ===")
        Log.d("ModalApiClient", "URL: $BASE_URL/generate_cases")
        Log.d("ModalApiClient", "Method: POST")
        Log.d("ModalApiClient", "Payload: ${payload.toString()}")
        Log.d(
            "ModalApiClient",
            "Cookies: ${client.cookieJar.loadForRequest("$BASE_URL/generate_cases".toHttpUrl())}"
        )

        val requestBody =
            payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$BASE_URL/generate_cases")
            .post(requestBody)
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string()
                    val cookies = it.headers("Set-Cookie")

                    Log.d("ModalApiClient", "=== RESPONSE ===")
                    Log.d("ModalApiClient", "Status: ${it.code}")
                    Log.d("ModalApiClient", "Headers: ${it.headers}")
                    Log.d("ModalApiClient", "Cookies received: $cookies")
                    Log.d("ModalApiClient", "Body: $body")

                    if (!it.isSuccessful) {
                        val errorBody = body?: "Unknown error"
                        continuation.resumeWithException(IOException("HTTP ${it.code}: $errorBody"))
                        return
                    }


                    if (body == null) {
                        continuation.resumeWithException(IOException("Response body is null"))
                        return
                    }

                    try {
                        val casesResponse = gson.fromJson(body, GenerateCasesResponse::class.java)
                        continuation.resume(casesResponse.data)
                    } catch (e: JsonSyntaxException) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        })
    }

    suspend fun generateCasesWithRetry(
        language: String,
        subject: String,
        difficulty: String,
        questionType: String,
        subType: String,
        age: Int? = null,
        sex: String? = null,
        role: String? = null,
        answers: Map<String, String>? = null,
        maxRetries: Int = 3
    ): List<Case> {
        var lastException: Exception? = null

        for (attempt in 1..maxRetries) {
            try {
                return generateCases(
                    language = language,
                    subject = subject,
                    difficulty = difficulty,
                    questionType = questionType,
                    subType = subType,
                    age = age,
                    sex = sex,
                    role = role,
                    answers = answers
                )
            } catch (e: IOException) {
                lastException = e
                if (e.message?.contains("HTTP 500") == true && attempt < maxRetries) {
                    Log.w("ModalApiClient", "HTTP 500 on attempt $attempt, reinitializing session...")

                    // Reinitialize session completely
                    initializeSession()
                    delay(2000L * attempt) // Exponential backoff: 2s, 4s, 6s
                } else {
                    break
                }
            } catch (e: Exception) {
                lastException = e
                break // Don't retry for non-IO exceptions
            }
        }

        throw lastException ?: IOException("Failed after $maxRetries attempts")
    }

    // Add a method to check current turn
    suspend fun getCurrentTurn(): Int = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url("$BASE_URL/generate_cases") // This endpoint returns turn info on GET
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                // The GET request to this endpoint might return turn info
                // Or you might need to parse it from error messages
                continuation.resume(1) // Default to 1 if we can't determine
            }

        })
    }
    /**
     * Start asynchronous analysis
     */
    suspend fun startAnalysis(
        role: String,
        questionType: String,
        language: String
    ): String = suspendCancellableCoroutine { continuation ->

        val payload = JSONObject().apply {
            put("role", role)
            put("question_type", questionType)
            put("language", language)
        }

        val requestBody =
            payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$BASE_URL/start_analysis")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        val errorBody = it.body?.string() ?: "Unknown error"
                        continuation.resumeWithException(IOException("HTTP ${it.code}: $errorBody"))
                        return
                    }

                    val body = it.body?.string()
                    if (body == null) {
                        continuation.resumeWithException(IOException("Response body is null"))
                        return
                    }

                    try {
                        val analysisResponse =
                            gson.fromJson(body, StartAnalysisResponse::class.java)
                        continuation.resume(analysisResponse.job_id)
                    } catch (e: JsonSyntaxException) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        })
    }

    /**
     * Check analysis job status
     */
    suspend fun getAnalysisStatus(jobId: String): AnalysisStatusResponse =
        suspendCancellableCoroutine { continuation ->
            val request = Request.Builder()
                .url("$BASE_URL/get_analysis_status/$jobId")
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!it.isSuccessful) {
                            val errorBody = it.body?.string() ?: "Unknown error"
                            continuation.resumeWithException(IOException("HTTP ${it.code}: $errorBody"))
                            return
                        }

                        val body = it.body?.string()
                        if (body == null) {
                            continuation.resumeWithException(IOException("Response body is null"))
                            return
                        }

                        try {
                            val statusResponse =
                                gson.fromJson(body, AnalysisStatusResponse::class.java)
                            continuation.resume(statusResponse)
                        } catch (e: JsonSyntaxException) {
                            continuation.resumeWithException(e)
                        }
                    }
                }
            })
        }

    /**
     * Submit responses and get immediate analysis (synchronous)
     */
    suspend fun submitResponses(
        answers: Map<String, String>,
        role: String,
        questionType: String,
        subType: String,
        language: String
    ): AnalysisResultResponse = suspendCancellableCoroutine { continuation ->

        val payload = JSONObject().apply {
            put("answers", JSONObject(answers))
            put("role", role)
            put("question_type", questionType)
            put("sub_type", subType)
            put("language", language)
        }

        val requestBody =
            payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$BASE_URL/submit_responses")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        val errorBody = it.body?.string() ?: "Unknown error"
                        continuation.resumeWithException(IOException("HTTP ${it.code}: $errorBody"))
                        return
                    }

                    val body = it.body?.string()
                    if (body == null) {
                        continuation.resumeWithException(IOException("Response body is null"))
                        return
                    }

                    try {
                        val analysisResponse =
                            gson.fromJson(body, AnalysisResultResponse::class.java)
                        continuation.resume(analysisResponse)
                    } catch (e: JsonSyntaxException) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        })
    }

    /**
     * Reset user session
     */
    suspend fun resetSession(): Boolean = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url("$BASE_URL/reset")
            .post(RequestBody.create(null, ByteArray(0)))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    continuation.resume(it.isSuccessful)
                }
            }
        })
    }

    /**
     * Chat with AI
     */
    suspend fun converse(text: String): String = suspendCancellableCoroutine { continuation ->

        val payload = JSONObject().apply {
            put("text", text)
        }

        val requestBody =
            payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$BASE_URL/converse")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        val errorBody = it.body?.string() ?: "Unknown error"
                        continuation.resumeWithException(IOException("HTTP ${it.code}: $errorBody"))
                        return
                    }

                    val body = it.body?.string()
                    if (body == null) {
                        continuation.resumeWithException(IOException("Response body is null"))
                        return
                    }

                    try {
                        val jsonResponse = JSONObject(body)
                        val responseText = jsonResponse.getString("response")
                        continuation.resume(responseText)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        })
    }

    private fun extractUserIdFromCookie(cookie: String?): String? {
        // This is a simplified example - you'll need to parse the actual cookie format
        return cookie?.substringAfter("user_id=")?.substringBefore(";")
    }
}