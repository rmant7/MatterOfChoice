package com.matterofchoice.api

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.AtomicReference
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.matterofchoice.model.Case
import com.matterofchoice.model.Option
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.parcelize.Parcelize
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
//import okhttp3.java.net.cookiejar.JavaNetCookieJar
import org.json.JSONObject
import java.io.IOException
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


// CookieJar adapter backed by java.net.CookieManager (works even if JavaNetCookieJar is unavailable)
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

import java.net.HttpCookie
import java.net.URI

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

    /**
     * startCaseGeneration used to start a job; with the new server it should return the generated cases immediately.
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
