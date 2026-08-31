package com.keepnc.ui.auth

import androidx.annotation.StringRes

/**
 * Represents every possible state of the login screen.
 */
sealed class LoginState {
    /** Initial state — form is idle, waiting for user input. */
    object Idle : LoginState()

    /** Talking to the server (initiating login flow). */
    object Loading : LoginState()

    /**
     * The server responded. The browser has been opened at [loginUrl].
     * We are now polling in the background waiting for the user to authorize.
     */
    data class BrowserOpened(val loginUrl: String) : LoginState()

    /** Credentials received and saved. Navigate to main screen. */
    object Success : LoginState()

    /** Something went wrong. Show localized [messageRes] or fallback [message] to the user. */
    data class Error(
        val message: String? = null,
        @StringRes val messageRes: Int? = null
    ) : LoginState()
}
