package com.keepnc.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepnc.data.auth.Credentials
import com.keepnc.data.auth.LoginFlowService
import com.keepnc.data.auth.TokenStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [LoginActivity].
 *
 * Survives configuration changes (screen rotations) — if the user rotates the screen
 * while waiting for browser auth, polling continues uninterrupted.
 *
 * BEGINNER NOTE: ViewModels must NOT hold references to Activities, Fragments, or Views —
 * those get destroyed on rotation, which would cause memory leaks.
 * Instead, the Activity observes [loginState] and reacts to changes.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginFlowService: LoginFlowService,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun isLoggedIn(): Boolean = tokenStorage.isLoggedIn()

    /**
     * Kicks off the Login Flow v2 sequence.
     *
     * Validates the URL, initiates the flow with the server, signals the Activity
     * to open the browser, then polls for credentials.
     *
     * Everything runs inside [viewModelScope] on the default dispatcher (IO for network).
     */
    fun startLoginFlow(rawUrl: String) {
        // Basic client-side validation before hitting the network
        val serverUrl = rawUrl.trim().trimEnd('/')
        if (serverUrl.isBlank()) {
            _loginState.value = LoginState.Error(messageRes = com.keepnc.R.string.login_error_empty_url)
            return
        }
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            _loginState.value = LoginState.Error(messageRes = com.keepnc.R.string.login_error_invalid_url)
            return
        }

        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading

                // Step 1: Ask the server to start the flow
                val flowResponse = loginFlowService.initiateLoginFlow(serverUrl)

                // Step 2: Tell the Activity to open the browser
                // (the Activity opens the URL; we start polling immediately)
                _loginState.value = LoginState.BrowserOpened(flowResponse.login)

                // Step 3: Poll until credentials arrive (or timeout after 5 minutes)
                val creds = loginFlowService.pollForCredentials(
                    endpoint = flowResponse.poll.endpoint,
                    token = flowResponse.poll.token
                )

                // Step 4: Persist credentials securely
                tokenStorage.saveCredentials(
                    Credentials(
                        serverUrl = creds.server.trimEnd('/'),
                        loginName = creds.loginName,
                        appPassword = creds.appPassword
                    )
                )

                _loginState.value = LoginState.Success

            } catch (e: java.util.concurrent.TimeoutException) {
                _loginState.value = LoginState.Error(messageRes = com.keepnc.R.string.login_error_timeout)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(
                    message = e.message
                )
            }
        }
    }
}
