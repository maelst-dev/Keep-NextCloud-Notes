package com.keepnc.data.auth

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeoutException
import javax.inject.Inject

// ---------------------------------------------------------------------------
// DTOs for Login Flow v2
// ---------------------------------------------------------------------------

/**
 * Response from POST /index.php/login/v2.
 * [login] — URL to open in the browser so the user can authorise the app.
 * [poll]  — polling info: keep POSTing to [poll.endpoint] with [poll.token]
 *            until credentials arrive.
 */
data class LoginFlowInitResponse(
    val poll: PollData,
    val login: String
)

data class PollData(
    val token: String,
    val endpoint: String
)

/**
 * Credential payload returned after the user approves the app in the browser.
 * [server]      — Nextcloud server URL
 * [loginName]   — username
 * [appPassword] — app-specific password (NOT the user's main password)
 */
data class LoginFlowCredentialsResponse(
    val server: String,
    val loginName: String,
    val appPassword: String
)

// ---------------------------------------------------------------------------
// Service
// ---------------------------------------------------------------------------

/**
 * Implements Nextcloud Login Flow v2.
 *
 * How it works:
 * 1. POST to {serverUrl}/index.php/login/v2 → get a browser URL + poll info
 * 2. Open the browser URL (handled by LoginActivity via Custom Tab)
 * 3. Keep POSTing to poll.endpoint every 2 seconds until the user approves
 * 4. On approval: receive {server, loginName, appPassword} → save to TokenStorage
 *
 * This service uses an UNAUTHENTICATED OkHttpClient — it doesn't attach any
 * auth headers because we don't have credentials yet at this stage.
 *
 * Reference: https://docs.nextcloud.com/server/latest/developer_manual/client_apis/LoginFlow/index.html
 */
class LoginFlowService(
    private val httpClient: OkHttpClient,
    private val gson: Gson
) {
    /**
     * Step 1: Initiate the login flow.
     *
     * POSTs to the server and returns the browser URL + polling data.
     * Throws an exception if the server is unreachable or returns an error.
     */
    suspend fun initiateLoginFlow(serverUrl: String): LoginFlowInitResponse =
        withContext(Dispatchers.IO) {
            val url = "$serverUrl/index.php/login/v2"
            // POST with empty body — the server only needs the endpoint hit
            val request = Request.Builder()
                .url(url)
                .post(FormBody.Builder().build())
                .build()

            val responseBody = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Server returned ${response.code}: ${response.message}")
                }
                response.body?.string() ?: throw IllegalStateException("Empty response from server")
            }

            gson.fromJson(responseBody, LoginFlowInitResponse::class.java)
        }

    /**
     * Step 3: Poll for credentials.
     *
     * Keeps POSTing to [endpoint] with the [token] in the form body.
     * - 200 → credentials ready, parse and return
     * - 404 → user hasn't approved yet, wait 2 seconds and retry
     * - 5-minute timeout → throw [TimeoutException]
     *
     * BEGINNER NOTE: This is a suspending function — the `delay(2000)` does NOT
     * block any thread. The coroutine is simply paused and resumed after 2 seconds.
     */
    suspend fun pollForCredentials(endpoint: String, token: String): LoginFlowCredentialsResponse {
        val formBody = FormBody.Builder()
            .add("token", token)
            .build()

        val timeoutMs = 5 * 60 * 1000L // 5 minutes
        val startTime = System.currentTimeMillis()

        while (true) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw TimeoutException("Login timed out after 5 minutes")
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(formBody)
                .build()

            // Each HTTP call is blocking — switch to IO for the duration of execute()
            val (code, body) = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute().use { response ->
                    response.code to response.body?.string()
                }
            }

            when {
                code == 200 && body != null -> {
                    return gson.fromJson(body, LoginFlowCredentialsResponse::class.java)
                }
                code == 404 -> {
                    // User hasn't approved yet — suspend (not block) for 2 seconds
                    delay(2000)
                }
                else -> {
                    throw IllegalStateException("Unexpected poll response: $code")
                }
            }
        }
    }
}
