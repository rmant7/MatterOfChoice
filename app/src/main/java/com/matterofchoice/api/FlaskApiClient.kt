package com.matterofchoice.api


import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.matterofchoice.model.Case
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Data classes to cleanly parse the JSON responses from our server
data class StartJobResponse(val job_id: String)
data class JobStatusResponse(
    val status: String,
    val result: List<Case>?, // The list of cases when status is 'complete'
    val error: String?
)
data class AnalysisResponse(val analysis: String) // Assuming the analysis is a simple string

data class AnalysisRequest(
    val cases: List<Case>,
    val user_choices: Map<String, String>,
    val role: String,
    val question_type: String,
    val language: String
)


/**
 * A singleton object to handle all communication with our Flask backend.
 * Using an 'object' ensures we only have one instanhttps://ajkg123.pythonanywhere.com/casesce of OkHttpClient.
 */
object FlaskApiClient {

    // IMPORTANT: This is the development URL. For production, replace this with your deployed server's address.
    private const val BASE_URL = "https://jackandjill.pythonanywhere.com/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(160, TimeUnit.SECONDS)
        .readTimeout(160, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Calls the /start_case_generation endpoint.
     * This is a suspend function, making it easy to call from a coroutine.
     * @return The job_id for the background task.
     * @throws Exception if the network call fails or the server returns an error.
     */
    suspend fun startCaseGeneration(payload: JSONObject): String = suspendCancellableCoroutine { continuation ->
        val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$BASE_URL/start_case_generation")
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
                        continuation.resumeWithException(IOException("Unexpected code ${it.code}: ${it.body?.string()}"))
                        return
                    }

                    val body = it.body?.string()
                    if (body == null) {
                        continuation.resumeWithException(IOException("Response body is null"))
                        return
                    }

                    try {
                        val jobResponse = gson.fromJson(body, StartJobResponse::class.java)
                        continuation.resume(jobResponse.job_id)
                    } catch (e: JsonSyntaxException) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        })
        continuation.invokeOnCancellation {
            client.newCall(request).cancel()
        }
    }

    /**
     * Calls the /get_job_status endpoint to poll for results.
     * @param jobId The ID of the job to check.
     * @return A JobStatusResponse object with the current status and data.
     * @throws Exception on network or parsing errors.
     */
    suspend fun getJobStatus(jobId: String): JobStatusResponse = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url("$BASE_URL/get_job_status/$jobId")
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
                        continuation.resumeWithException(IOException("Unexpected code ${it.code}: ${it.body?.string()}"))
                        return
                    }
                    val body = it.body?.string()
                    if (body == null) {
                        continuation.resumeWithException(IOException("Response body is null"))
                        return
                    }
                    try {
                        val statusResponse = gson.fromJson(body, JobStatusResponse::class.java)
                        continuation.resume(statusResponse)
                    } catch (e: JsonSyntaxException) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        })
        continuation.invokeOnCancellation {
            client.newCall(request).cancel()
        }
    }
    suspend fun postAnalysis(analysisRequest: AnalysisRequest): AnalysisResponse = suspendCancellableCoroutine { continuation ->
        val requestBody = gson.toJson(analysisRequest).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$BASE_URL/analysis")
            .post(requestBody)
            .build()
        Log.d("FlaskApiClient", "listening for post")
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {

                Log.d("FlaksApiClient","Analysis failed with ${e.toString()}")
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
                Log.d("FlaksApiClient","Analysis failed with ${e.toString()}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("FlaskApiClient", "listening for response ${response.code}")
                response.use {
                    val body = it.body?.string()
                    if (!it.isSuccessful) {
                        val errorBody = body
                        val message = if (errorBody != null) {
                            try {
                                val json = JSONObject(errorBody)
                                json.getJSONObject("error").getString("message")
                            } catch (e: Exception) {
                                "Unexpected error format"
                            }
                        } else {
                            "Http error ${it.code}"
                        }
                        Log.d("FlaksApiClient","Analysis failed with $message")
                        continuation.resumeWithException(IOException(message))
                        return
                    }


                    if (body == null) {
                        continuation.resumeWithException(IOException("Response body is null"))
                        return
                    }

                    try {
                        val analysisResponse = gson.fromJson(body, AnalysisResponse::class.java)
                        continuation.resume(analysisResponse)
                    } catch (e: JsonSyntaxException) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        })
        continuation.invokeOnCancellation {
            client.newCall(request).cancel()
        }
    }

}