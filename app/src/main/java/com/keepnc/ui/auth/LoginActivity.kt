package com.keepnc.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.keepnc.databinding.ActivityLoginBinding
import com.keepnc.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Entry point for authentication.
 *
 * If the user is already logged in, skip straight to [MainActivity].
 * Otherwise, show a server URL field and launch Login Flow v2 in a Custom Tab.
 *
 * Login Flow v2 steps (from this Activity's perspective):
 * 1. User types server URL and taps "Login"
 * 2. ViewModel calls LoginFlowService → gets a browser URL
 * 3. We open that URL in a Custom Chrome Tab (system browser, branded)
 * 4. The user authenticates in the browser
 * 5. ViewModel polls for credentials in the background
 * 6. On success → start MainActivity
 *
 * BEGINNER NOTE: We use `repeatOnLifecycle(Lifecycle.State.STARTED)` to collect
 * StateFlow safely — this automatically pauses collection when the activity is
 * in the background and resumes when it comes back. This prevents processing
 * UI updates while the screen is invisible.
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If already authenticated, go directly to the main screen
        if (viewModel.isLoggedIn()) {
            startMainActivity()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeLoginState()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val url = binding.etServerUrl.text?.toString()?.trim() ?: ""
            viewModel.startLoginFlow(url)
        }
    }

    private fun observeLoginState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    handleLoginState(state)
                }
            }
        }
    }

    private fun handleLoginState(state: LoginState) {
        // Reset visible state
        binding.progressBar.visibility = View.GONE
        binding.tvStatus.visibility = View.GONE
        binding.btnLogin.isEnabled = true

        when (state) {
            is LoginState.Idle -> Unit // nothing to do

            is LoginState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.btnLogin.isEnabled = false
            }

            is LoginState.BrowserOpened -> {
                // Show polling status message
                binding.tvStatus.visibility = View.VISIBLE
                binding.tvStatus.text = getString(com.keepnc.R.string.login_waiting)
                binding.progressBar.visibility = View.VISIBLE
                binding.btnLogin.isEnabled = false

                // Open the Nextcloud authorization page in a Custom Tab.
                // Custom Tabs use the system browser with a branded toolbar —
                // safer than WebView (user can see the URL) and better UX than external browser.
                openInCustomTab(state.loginUrl)
            }

            is LoginState.Success -> {
                startMainActivity()
            }

            is LoginState.Error -> {
                val text = if (state.messageRes != null) {
                    getString(state.messageRes)
                } else {
                    state.message ?: getString(com.keepnc.R.string.error_generic)
                }
                Snackbar.make(binding.root, text, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun openInCustomTab(url: String) {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(this, url.toUri())
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish() // remove LoginActivity from the back stack
    }
}
