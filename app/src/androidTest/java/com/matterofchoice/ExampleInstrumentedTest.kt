//package com.matterofchoice
//
//import android.webkit.CookieManager
//import androidx.test.platform.app.InstrumentationRegistry
//import androidx.test.ext.junit.runners.AndroidJUnit4
//
//import org.junit.Test
//import org.junit.runner.RunWith
//
//import org.junit.Assert.*
//
///**
// * Instrumented test, which will execute on an Android device.
// *
// * See [testing documentation](http://d.android.com/tools/testing).
// */
////@RunWith(AndroidJUnit4::class)
////class ExampleInstrumentedTest {
////    @Test
////    fun useAppContext() {
////        // Context of the app under test.
////        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
////        assertEquals("com.matterofchoice", appContext.packageName)
////    }
////}
//
////
////import androidx.test.ext.junit.runners.AndroidJUnit4
////import androidx.test.platform.app.InstrumentationRegistry
//import okhttp3.*
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.RequestBody.Companion.asRequestBody
//import okhttp3.RequestBody.Companion.toRequestBody
//import org.json.JSONObject // This will now work as it's an instrumented test
//import org.junit.Assert.*
//
//import java.io.File
//import java.util.concurrent.TimeUnit
//
///**
// * Instrumented test, which will execute on an Android device.
// *
// * See [testing documentation](http://d.android.com/tools/testing).
// */
//@RunWith(AndroidJUnit4::class)
//class ExampleInstrumentedTest {
//
//    // You can keep OkHttpFlaskClient as a separate class or nest it if preferred
//    // For clarity, I'll keep it separate here.
//    // If it's only used by this test, you could make it an inner class.
//
//    @Test
//    fun testOkHttpFlaskClient_generateCases() {
//        // Context can be useful for instrumented tests, e.g., for file access from assets
//        // val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//        // assertEquals("com.matterofchoice", appContext.packageName) // Example assertion
//
//        val client = OkHttpFlaskClient()
//        val testJson = JSONObject()
//        testJson.put("language", "en")
//        testJson.put("subject", "math")
//        testJson.put("difficulty", "easy")
//        testJson.put("question_type", "behavioral")
//        testJson.put("sub_type", "multiple_choice")
//// Add any other optional fields as needed
////        response = client.generateCases(testJson)
//
//        var response: Response? = null
//        try {
//            response = client.generateCases(testJson)
//            println(response
//            )
//            assertNotNull("Response should not be null", response)
//            assertTrue("Response should be successful", response!!.isSuccessful) // Note the double bang, ensure response is not null first
//
//            val responseBody = response.body?.string()
//            assertNotNull("Response body should not be null", responseBody)
//            println("Response from generateCases: $responseBody")
//            // Add more specific assertions based on the expected response content
//            // e.g., if you expect a JSON response:
//            // val jsonResponse = JSONObject(responseBody)
//            // assertEquals("expectedValue", jsonResponse.getString("expectedKey"))
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//            fail("Test failed due to exception: ${e.message}")
//        } finally {
//            response?.close() // Ensure the response body is closed
//        }
//    }
//
//    @Test
//    fun testOkHttpFlaskClient_reset() {
//        val client = OkHttpFlaskClient()
//        var response: Response? = null
//        try {
//            response = client.reset()
//            assertNotNull("Response should not be null", response)
//            assertTrue("Response should be successful", response!!.isSuccessful)
//            val responseBody = response.body?.string()
//            assertNotNull("Response body should not be null", responseBody)
//            println("Response from reset: $responseBody")
//            // Add assertions based on expected response
//        } catch (e: Exception) {
//            e.printStackTrace()
//            fail("Test failed due to exception: ${e.message}")
//        } finally {
//            response?.close()
//        }
//    }
//
//    // Add more tests for other OkHttpFlaskClient methods (getImage, converse, analyzeImage, etc.)
//    // For analyzeImage, you'd need to handle creating a dummy File for testing purposes.
//    // This might involve writing a file to the app's cache directory.
//    @Test
//    fun testOkHttpFlaskClient_analyzeImage() {
//        val client =   OkHttpFlaskClient()
//        var response: Response? = null
//        try {
//            // For instrumented tests, you can create a temporary file in the app's cache
//            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//            val tempFile = File.createTempFile("test_image", ".png", appContext.cacheDir)
//            val pngData = byteArrayOf(
//                -119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0, 31, 21, -60, -119, 0, 0, 0, 11, 73, 68, 65, 84, 120, -38, 99, 96, 0, 0, 0, 2, 0, 1, -27, -104, 53, 7, 0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126
//            )
//            tempFile.writeBytes(pngData)
//
//            val prompt = "Describe this test image"
//
//            response = client.analyzeImage(tempFile, prompt)
//            assertNotNull("Response should not be null", response)
//            assertTrue("Response should be successful", response!!.isSuccessful)
//            val responseBody = response.body?.string()
//            assertNotNull("Response body should not be null", responseBody)
//            println("Response from analyzeImage: $responseBody")
//
//            tempFile.delete() // Clean up the temporary file
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//            fail("Test failed due to exception: ${e.message}")
//        } finally {
//            response?.close()
//        }
//    }
//}
//
//// The OkHttpFlaskClient class itself doesn't need many changes,
//// as it was already using OkHttp which works in both environments.
//// The main difference is that org.json.JSONObject is available in instrumented tests.
//class OkHttpFlaskClient {
//    private val baseUrl: String =
//        "https://congenial-palm-tree-57qvwq7gjp9h4xrp-5000.app.github.dev" // Replace with your actual test server URL if needed
//    private val client = OkHttpClient.Builder()
//        .connectTimeout(190, TimeUnit.SECONDS)
//        .readTimeout(190, TimeUnit.SECONDS)
//        .build()
//
//    // 1. POST /generate_cases
//    fun generateCases(json: JSONObject, sessionCookie: String? = null): Response? {
//        val body =
//            json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
//        val request = Request.Builder()
//            .url("$baseUrl/generate_cases")
//            .post(body)
//            .apply { sessionCookie?.let { addHeader("Cookie", it) } }
//            .build()
//        return client.newCall(request).execute()
//    }
//
//    // 2. GET /get_image/<case_id>
//    fun getImage(caseId: String, sessionCookie: String? = null): Response? {
//        val request = Request.Builder()
//            .url("$baseUrl/get_image/$caseId")
//            .get()
//            .apply { sessionCookie?.let { addHeader("Cookie", it) } }
//            .build()
//        return client.newCall(request).execute()
//    }
//
//    // 3. POST /reset
//    fun reset(sessionCookie: String? = null): Response? {
//        val request = Request.Builder()
//            .url("$baseUrl/reset")
//            .post("".toRequestBody()) // Empty body
//            .apply { sessionCookie?.let { addHeader("Cookie", it) } }
//            .build()
//        return client.newCall(request).execute()
//    }
//
//    // 4. POST /converse
//    fun converse(text: String, sessionCookie: String? = null): Response? {
//        val json = JSONObject().put("text", text)
//        val body =
//            json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
//        val request = Request.Builder()
//            .url("$baseUrl/converse")
//            .post(body)
//            .apply { sessionCookie?.let { addHeader("Cookie", it) } }
//            .build()
//        return client.newCall(request).execute()
//    }
//
//    // 5. POST /analyze-image
//    fun analyzeImage(imageFile: File, prompt: String, sessionCookie: String? = null): Response? {
//        val requestBody = MultipartBody.Builder()
//            .setType(MultipartBody.FORM)
//            .addFormDataPart(
//                "image",
//                imageFile.name,
//                imageFile.asRequestBody("image/*".toMediaTypeOrNull())
//            )
//            .addFormDataPart("prompt", prompt)
//            .build()
//        val request = Request.Builder()
//            .url("$baseUrl/analyze-image")
//            .post(requestBody) // This was incorrect before
//            .apply { sessionCookie?.let { addHeader("Cookie", it) } }
//            .build()
//
//
//
//        return client.newCall(request).execute()
//    }
//}
package com.matterofchoice

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File
import java.net.CookieManager
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    private val client = OkHttpFlaskClient()

    @Test
    fun testAsyncCaseGenerationFlow() {
        // This is the main test that validates the new, responsive backend flow.
        println("--- Testing: Asynchronous Case Generation Flow ---")

        // 1. Prepare the initial request data
        val initialJson = JSONObject().apply {
            put("language", "en")
            put("age", 25)
            put("subject", "workplace ethics")
            put("difficulty", "medium")
            put("question_type", "behavioral")
            put("sub_type", "scenario_analysis")
            put("sex", "unspecified")
        }

        var job_id: String? = null

        // 2. Call the '/start_case_generation' endpoint
        var startResponse: Response? = null
        try {
            println("Sending request to /start_case_generation...")
            startResponse = client.startCaseGeneration(initialJson)
            assertNotNull("Start response should not be null", startResponse)
            assertEquals("Start response should be 202 (Accepted)", 202, startResponse!!.code)

            val responseBody = startResponse.body?.string()
            assertNotNull("Start response body should not be null", responseBody)
            val jsonResponse = JSONObject(responseBody!!)
            assertTrue("Response should contain a 'job_id'", jsonResponse.has("job_id"))
            job_id = jsonResponse.getString("job_id")
            println("Successfully started job with ID: $job_id")

        } catch (e: Exception) {
            fail("Test failed during job start: ${e.message}")
        } finally {
            startResponse?.close()
        }

        assertNotNull("job_id should not be null after successful start", job_id)

        // 3. Poll the '/get_job_status' endpoint until completion
        var pollResponse: Response? = null
        var finalStatus: String? = null
        var finalResult: JSONObject? = null
        val maxPollTime = 150_000 // Max wait time in milliseconds (150 seconds)
        val pollInterval = 5_000L // Poll every 5 seconds
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < maxPollTime) {
            try {
                println("Polling status for job $job_id...")
                pollResponse = client.getJobStatus(job_id!!)
                assertNotNull("Poll response should not be null", pollResponse)
                assertTrue("Poll response should be successful", pollResponse!!.isSuccessful)

                val responseBody = pollResponse.body?.string()
                assertNotNull("Poll response body should not be null", responseBody)
                val jsonResponse = JSONObject(responseBody!!)
                val currentStatus = jsonResponse.getString("status")
                println("Current job status: $currentStatus")

                if (currentStatus == "complete") {
                    finalStatus = "complete"
                    assertTrue("Completed job should have a 'result' key", jsonResponse.has("result"))
                    finalResult = jsonResponse
                    break // Exit the loop on success
                } else if (currentStatus == "failed") {
                    finalStatus = "failed"
                    fail("Job failed on the server: ${jsonResponse.optString("error", "Unknown error")}")
                    break
                }

                // Wait before polling again
                Thread.sleep(pollInterval)

            } catch (e: Exception) {
                fail("Test failed during polling: ${e.message}")
            } finally {
                pollResponse?.close()
            }
        }

        // 4. Assert the final state
        if (finalStatus == null) {
            fail("Test timed out after ${maxPollTime / 1000} seconds. Job never completed.")
        }

        assertEquals("Final job status should be 'complete'", "complete", finalStatus)
        assertNotNull("Final result should not be null", finalResult)

        val caseData = finalResult!!.getJSONArray("result")
        assertTrue("Result should contain at least one case", caseData.length() > 0)
        println("Successfully received ${caseData.length()} cases.")
        println("Final data: ${caseData.toString(2)}")
    }

    // You can keep your other tests like 'testReset' and 'testAnalyzeImage' as they are.
    // They don't need to change.
}


// =======================================================================================
// === FIX: Update OkHttpFlaskClient with new async methods ==============================
// =======================================================================================

class OkHttpFlaskClient {
    private val baseUrl: String = "https://congenial-palm-tree-57qvwq7gjp9h4xrp-5000.app.github.dev"

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS) // A generous timeout is still good practice
        .readTimeout(120, TimeUnit.SECONDS)
//        .cookieJar(JavaNetCookieJar(CookieManager())) // Important for session management
        .build()

    // NEW Method 1: Start the generation job
    fun startCaseGeneration(json: JSONObject): Response? {
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$baseUrl/start_case_generation")
            .post(body)
            .build()
        return client.newCall(request).execute()
    }

    // NEW Method 2: Get the status of the job
    fun getJobStatus(jobId: String): Response? {
        val request = Request.Builder()
            .url("$baseUrl/get_job_status/$jobId")
            .get()
            .build()
        return client.newCall(request).execute()
    }

    // --- OLD /generate_cases is no longer used by this test flow ---
    // You can remove it or keep it for legacy purposes.
    // fun generateCases(json: JSONObject): Response? { ... }

    // Other methods remain unchanged
    fun reset(): Response? {
        val body = "".toRequestBody()
        val request = Request.Builder()
            .url("$baseUrl/reset")
            .post(body)
            .build()
        return client.newCall(request).execute()
    }

//    fun analyzeImage(imageFile: File, prompt: String): Response? {
//        val requestBody = MultipartBody.Builder()
//            .setType(MultipartBody.FORM)
//            .addFormDataPart("image", imageFile.name, imageFile.asRequestBody("image/*".toMediaTypeOrNull()))
//            .addFormDataPart("prompt", prompt)
//            .build()
//        val request = Request.Builder()
//            .url("$baseUrl/analyze-image")
//            .post(requestBody)
//            .build()
//        return client.newCall(request).execute()
//    }
}