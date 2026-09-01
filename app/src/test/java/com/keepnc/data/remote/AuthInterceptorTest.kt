package com.keepnc.data.remote

import com.keepnc.data.auth.Credentials
import com.keepnc.data.auth.TokenStorage
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AuthInterceptorTest {

    private class FakeChain(private val initialRequest: Request) : Interceptor.Chain {
        var interceptedRequest: Request? = null

        override fun request(): Request = initialRequest

        override fun proceed(request: Request): Response {
            interceptedRequest = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        override fun connection(): okhttp3.Connection? = null
        override fun call(): okhttp3.Call = throw NotImplementedError()
        override fun connectTimeoutMillis(): Int = 0
        override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
        override fun readTimeoutMillis(): Int = 0
        override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 0
        override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
    }

    private fun createDummyTokenStorage(serverUrl: String, loginName: String = "user", appPassword: String = "pass"): TokenStorage {
        return object : TokenStorage() {
            override fun getServerUrl(): String = serverUrl
            override fun getCredentials(): Credentials = Credentials(serverUrl, loginName, appPassword)
            override fun isLoggedIn(): Boolean = true
        }
    }

    @Test
    fun `intercept rewrites placeholder URL with full path to stored server URL`() {
        val tokenStorage = createDummyTokenStorage("https://my-nextcloud.com")
        val interceptor = AuthInterceptor(tokenStorage)

        val request = Request.Builder()
            .url("https://placeholder.invalid/index.php/apps/notes/api/v1/notes")
            .build()

        val chain = FakeChain(request)
        interceptor.intercept(chain)

        val finalRequest = chain.interceptedRequest
        assertNotNull(finalRequest)
        assertEquals("https://my-nextcloud.com/index.php/apps/notes/api/v1/notes", finalRequest?.url.toString())
        assertNotNull(finalRequest?.header("Authorization"))
        assertEquals("true", finalRequest?.header("OCS-APIREQUEST"))
    }

    @Test
    fun `intercept rewrites placeholder URL without v1 prefix via fallback`() {
        val tokenStorage = createDummyTokenStorage("https://my-nextcloud.com")
        val interceptor = AuthInterceptor(tokenStorage)

        val request = Request.Builder()
            .url("https://placeholder.invalid/notes/42")
            .build()

        val chain = FakeChain(request)
        interceptor.intercept(chain)

        val finalRequest = chain.interceptedRequest
        assertNotNull(finalRequest)
        assertEquals("https://my-nextcloud.com/index.php/apps/notes/api/v1/notes/42", finalRequest?.url.toString())
    }
}
