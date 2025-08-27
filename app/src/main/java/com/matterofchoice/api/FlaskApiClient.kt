package com.matterofchoice.api

//import okhttp3.java.net.cookiejar.JavaNetCookieJar


// CookieJar adapter backed by java.net.CookieManager (works even if JavaNetCookieJar is unavailable)

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.AtomicReference
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.matterofchoice.model.Case
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.parcelize.Parcelize
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
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class JavaNetCookieJarAdapter(private val cookieManager: CookieManager = CookieManager()): CookieJar {

    init {
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val uri = URI(url.scheme + "://" + url.host)
        for (cookie in cookies) {
            val httpCookie = HttpCookie(cookie.name, cookie.value).apply {
                path = cookie.path
                domain = cookie.domain
                secure = cookie.secure
                isHttpOnly = cookie.httpOnly
                // maxAge unknown here — leave default (session) or set if you want
            }
            cookieManager.cookieStore.add(uri, httpCookie)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val uri = URI(url.scheme + "://" + url.host)
        val store = cookieManager.cookieStore.get(uri)
        val result = mutableListOf<Cookie>()
        for (hc in store) {
            // httpCookie.domain can be null; fall back to request host
            val domain = hc.domain ?: url.host
            val path = hc.path ?: "/"
            val builder = Cookie.Builder()
                .name(hc.name)
                .value(hc.value)
                .domain(domain)
                .path(path)
            if (hc.secure) builder.secure()
            if (hc.isHttpOnly) builder.httpOnly()
            result.add(builder.build())
        }
        return result
    }
}


private val cookieManager = CookieManager().apply {
    setCookiePolicy(CookiePolicy.ACCEPT_ALL)

}
private val client = OkHttpClient.Builder()
    .cookieJar(JavaNetCookieJarAdapter(cookieManager))
    .connectTimeout(160, TimeUnit.SECONDS)
    .readTimeout(160, TimeUnit.SECONDS)
    .build()

// Response wrapper for /generate_cases endpoint: { "data": [...] }
data class GenerateCasesResponse(val data: List<Case>?, val message: String? = null, val error: String? = null)

// Job status / analysis-related response models (kept lightweight to match server)
data class StartAnalysisResponse(val job_id: String?)
data class JobStatusResponse(
    val status: String,
    val result: JsonElement? = null,
    val error: String? = null
)



@Parcelize
data class Option(
    val number: Int,                  // The option number
    val option: String,               // The description of the option
    val health: Int,                  // Health impact of this option
    val wealth: Int,                  // Wealth impact of this option
    val relationships: Int,           // Relationship impact of this option
    val happiness: Int,               // Happiness impact of this option
    val knowledge: Int,               // Knowledge impact of this option
    val karma: Int,                   // Karma impact of this option
    val timeManagement: Int,         // Time management impact of this option
    val environmentalImpact: Int,    // Environmental impact of this option
    val personalGrowth: Int,         // Personal growth impact of this option
    val socialResponsibility: Int    // Social responsibility impact of this option
): Parcelable

data class AnalysisRequest(
//    val user_id: String,               // << server requires user_id
    val cases: List<Case>,
    val user_choices: Map<String, String>,
    val role: String,
    val question_type: String,
    val language: String
)

data class AnalysisResponse(
    val overall_judgement: String?,
    val cases: List<Any>?
)


object FlaskApiClient {

    private const val BASE_URL = "https://reqeique--matter-of-choice-v-fastapi-app.modal.run/"

    private val gson = Gson()


   /*  * startCaseGeneration used to start a job; with the new server it should return the generated cases immediately.
     * Now this method calls POST /generate_cases and returns the List<Case> produced by the server.
     */

    // Returns server session user_id set by GET /cases
    suspend fun getSessionUserId(): String = suspendCancellableCoroutine { cont ->
        val request = Request.Builder()
            .url("${BASE_URL}get_user_id")
            .get()
            .build()
        val call = client.newCall(request)
        cont.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!cont.isActive) return
                cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!cont.isActive) { response.close(); return }
                response.use {
                    val body = it.body?.string()
                    if (!it.isSuccessful) {
                        cont.resumeWithException(IOException("HTTP ${it.code}: $body"))
                        return
                    }
                    try {
                        val obj = gson.fromJson(body, Map::class.java)
                        val userId = (obj["user_id"] as? String) ?: throw IOException("No user_id in response")
                        cont.resume(userId)
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                }
            }
        })
    }

    suspend fun startCaseGeneration(payload: JSONObject): List<Case> = suspendCancellableCoroutine { cont ->
        val getReq = Request.Builder()
            .url("${BASE_URL}cases")
            .get()
            .build()

        val getCall = client.newCall(getReq)
        val postCallRef = AtomicReference<Call?>(null)

        // Ensure underlying calls are cancelled if coroutine cancelled
        cont.invokeOnCancellation {
            getCall.cancel()
            postCallRef.get()?.cancel()
        }

        getCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!cont.isActive) return
                cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                // We don't need the /cases body; it just sets the session cookie on the client.
                response.close()
                if (!cont.isActive) return

                // Now make the POST /generate_cases with the same client (cookie jar preserves session)
                val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val postReq = Request.Builder()
                    .url("${BASE_URL}generate_cases")
                    .post(requestBody)
                    .build()

                val postCall = client.newCall(postReq)
                postCallRef.set(postCall)

                postCall.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (!cont.isActive) return
                        cont.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (!cont.isActive) {
                            response.close()
                            return
                        }

                        response.use {
                            val body = it.body?.string()
                            if (!it.isSuccessful) {
                                cont.resumeWithException(IOException("Unexpected HTTP ${it.code}: $body"))
                                return
                            }

                            if (body == null) {
                                cont.resumeWithException(IOException("Response body is null"))
                                return
                            }

                            try {
                                val wrapper = gson.fromJson(body, GenerateCasesResponse::class.java)
                                val cases = wrapper.data ?: emptyList()
                                cont.resume(cases)
                            } catch (e: JsonSyntaxException) {
                                cont.resumeWithException(e)
                            } catch (e: Exception) {
                                cont.resumeWithException(e)
                            }
                        }
                    }
                })
            }
        })
    }

    suspend fun postAnalysis(analysisRequest: AnalysisRequest): String = suspendCancellableCoroutine { cont ->
        val requestBody = gson.toJson(analysisRequest)
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("${BASE_URL}start_analysis")
            .post(requestBody)
            .build()

        val call = client.newCall(request)
        cont.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!cont.isActive) return
                cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!cont.isActive) { response.close(); return }
                response.use {
                    val body = it.body?.string()
                    if (!it.isSuccessful) {
                        cont.resumeWithException(IOException("HTTP ${it.code}: $body"))
                        return
                    }
                    if (body == null) {
                        cont.resumeWithException(IOException("Response body is null"))
                        return
                    }
                    try {
                        val startResp = gson.fromJson(body, StartAnalysisResponse::class.java)
                        val jobId = startResp.job_id ?: throw IOException("No job_id in response")
                        cont.resume(jobId)
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                }
            }
        })
    }

    suspend fun getJobStatus(jobId: String): JobStatusResponse = suspendCancellableCoroutine { cont ->
        val request = Request.Builder()
            .url("${BASE_URL}get_analysis_status/$jobId")
            .get()
            .build()

        val call = client.newCall(request)
        cont.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!cont.isActive) return
                cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!cont.isActive) { response.close(); return }
                response.use {
                    val body = it.body?.string()
                    if (!it.isSuccessful) {
                        cont.resumeWithException(IOException("HTTP ${it.code}: $body"))
                        return
                    }
                    if (body == null) {
                        cont.resumeWithException(IOException("Response body is null"))
                        return
                    }
                    try {
                        // parse manually to keep 'result' flexible (JsonElement)
                        val jsonElem = JsonParser.parseString(body).asJsonObject
                        val status = if (jsonElem.has("status")) jsonElem.get("status").asString else "unknown"
                        val resultElem = if (jsonElem.has("result")) jsonElem.get("result") else null
                        val errorElem = if (jsonElem.has("error")) jsonElem.get("error").asString else null
                        cont.resume(JobStatusResponse(status = status, result = resultElem, error = errorElem))
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                }
            }
        })
    }

}


// Data classes for the new API responses
data class GenerateCasesResponsee(val data: List<Case>)

data class AnalysisResultResponsee(
    val overall_judgement: String,
    val cases: List<CaseAnalysise>
)

data class CaseAnalysise(
    val case_description: String,
    val player_choice: String,
    val optimal_choice: String,
    val analysis: String
)

data class StartAnalysisResponsee(val job_id: String)
data class AnalysisStatusResponse(
    val status: String,
    val result: AnalysisResultResponsee?,
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
                        val casesResponse = gson.fromJson(body, GenerateCasesResponsee::class.java)
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
                            gson.fromJson(body, StartAnalysisResponsee::class.java)
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
    ): AnalysisResultResponsee = suspendCancellableCoroutine { continuation ->

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
                            gson.fromJson(body, AnalysisResultResponsee::class.java)
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


