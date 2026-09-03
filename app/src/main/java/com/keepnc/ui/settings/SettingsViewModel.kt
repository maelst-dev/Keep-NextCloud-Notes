package com.keepnc.ui.settings

import androidx.lifecycle.ViewModel
import com.keepnc.BuildConfig
import com.keepnc.data.auth.TokenStorage
import com.keepnc.data.settings.FontSizePreset
import com.keepnc.data.settings.SettingsStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 *
 * Exposes user preferences as [StateFlow] and provides methods to update them.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStorage: SettingsStorage,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    val confirmSaveOnExit: StateFlow<Boolean> = settingsStorage.confirmSaveOnExit
    val confirmDeleteNote: StateFlow<Boolean> = settingsStorage.confirmDeleteNote
    val editorFontSize: StateFlow<FontSizePreset> = settingsStorage.editorFontSize
    val cardFontSize: StateFlow<FontSizePreset> = settingsStorage.cardFontSize
    val appLockEnabled: StateFlow<Boolean> = settingsStorage.appLockEnabled

    val serverUrl: String?
        get() = tokenStorage.getServerUrl()

    val appVersion: String
        get() = BuildConfig.VERSION_NAME

    fun setConfirmSaveOnExit(enabled: Boolean) {
        settingsStorage.setConfirmSaveOnExit(enabled)
    }

    fun setConfirmDeleteNote(enabled: Boolean) {
        settingsStorage.setConfirmDeleteNote(enabled)
    }

    fun setEditorFontSize(preset: FontSizePreset) {
        settingsStorage.setEditorFontSize(preset)
    }

    fun setCardFontSize(preset: FontSizePreset) {
        settingsStorage.setCardFontSize(preset)
    }

    fun setAppLockEnabled(enabled: Boolean) {
        settingsStorage.setAppLockEnabled(enabled)
    }
}
