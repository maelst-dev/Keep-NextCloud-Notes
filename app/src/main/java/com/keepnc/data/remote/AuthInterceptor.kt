package com.keepnc.data.remote

import android.util.Base64
import android.util.Log
import com.keepnc.data.auth.TokenStorage
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp interceptor that:
 * 1. Attaches HTTP Basic Auth to every request using credentials from [TokenStorage].
 * 2. Dynamically rewrites the request URL to the stored server URL so Retrofit
 *    works even if created before login or if the server URL changes.
 *
 * BEGINNER NOTE: OkHttp interceptors run on the background thread that made the
 * network call.
 */
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val credentials = tokenStorage.getCredentials()
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        if (credentials != null) {
            // Build "loginName:appPassword", base64-encode it, attach as Authorization header
            val encoded = Base64.encodeToString(
                "${credentials.loginName}:${credentials.appPassword}".toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )
            requestBuilder.header("Authorization", "Basic $encoded")
            // Required by Nextcloud OCS API endpoints
            requestBuilder.header("OCS-APIREQUEST", "true")
        }

        // Dynamic server URL resolution
        val serverUrl = tokenStorage.getServerUrl()?.trim()?.trimEnd('/')
        if (!serverUrl.isNullOrBlank()) {
            val targetBaseUrl = "$serverUrl/index.php/apps/notes/api/v1/".toHttpUrlOrNull()
            if (targetBaseUrl != null) {
                val originalUrl = originalRequest.url
                val newUrlBuilder = targetBaseUrl.newBuilder()
                // Find path relative to api/v1/
                val v1Index = originalUrl.pathSegments.indexOf("v1")
                val relativeSegments = if (v1Index != -1 && v1Index < originalUrl.pathSegments.size - 1) {
                    originalUrl.pathSegments.subList(
                        v1Index + 1,
                        originalUrl.pathSegments.size
                    )
                } else if (v1Index == -1) {
                    // Fallback if placeholder URL had no prefix — all segments are relative to baseUrl
                    originalUrl.pathSegments.filter { it.isNotBlank() }
                } else {
                    emptyList()
                }

                for (segment in relativeSegments) {
                    newUrlBuilder.addPathSegment(segment)
                }
                originalUrl.query?.let { newUrlBuilder.query(it) }
                requestBuilder.url(newUrlBuilder.build())
            }
        }

        val finalRequest = requestBuilder.build()
        Log.d("AuthInterceptor", "--> ${finalRequest.method} ${finalRequest.url}")
        val response = chain.proceed(finalRequest)
        Log.d("AuthInterceptor", "<-- ${response.code} ${finalRequest.url}")
        return response
    }
}
