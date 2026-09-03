package com.keepnc.data.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user preferences and settings for the app.
 *
 * Exposes reactive [StateFlow]s for the UI layer and synchronous getters
 * for immediate conditional checks during exit / delete actions.
 */
@Singleton
open class SettingsStorage {
    private val context: Context?
    private val injectedPrefs: SharedPreferences?

    @Inject
    constructor(@ApplicationContext context: Context) {
        this.context = context
        this.injectedPrefs = null
    }

    /** Constructor for unit testing without Android Context */
    constructor(sharedPreferences: SharedPreferences? = null) {
        this.context = null
        this.injectedPrefs = sharedPreferences
    }

    private val prefs: SharedPreferences by lazy {
        injectedPrefs ?: run {
            val ctx = checkNotNull(context) { "Context is required for SharedPreferences access" }
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private val _confirmSaveOnExit: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(prefs.getBoolean(KEY_CONFIRM_SAVE_ON_EXIT, DEFAULT_CONFIRM_SAVE_ON_EXIT))
    }
    open val confirmSaveOnExit: StateFlow<Boolean> by lazy { _confirmSaveOnExit.asStateFlow() }

    private val _confirmDeleteNote: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(prefs.getBoolean(KEY_CONFIRM_DELETE_NOTE, DEFAULT_CONFIRM_DELETE_NOTE))
    }
    open val confirmDeleteNote: StateFlow<Boolean> by lazy { _confirmDeleteNote.asStateFlow() }

    private val _editorFontSize: MutableStateFlow<FontSizePreset> by lazy {
        val key = prefs.getString(KEY_EDITOR_FONT_SIZE, FontSizePreset.DEFAULT.key)
        MutableStateFlow(FontSizePreset.fromKey(key))
    }
    open val editorFontSize: StateFlow<FontSizePreset> by lazy { _editorFontSize.asStateFlow() }

    private val _cardFontSize: MutableStateFlow<FontSizePreset> by lazy {
        val key = prefs.getString(KEY_CARD_FONT_SIZE, FontSizePreset.DEFAULT.key)
        MutableStateFlow(FontSizePreset.fromKey(key))
    }
    open val cardFontSize: StateFlow<FontSizePreset> by lazy { _cardFontSize.asStateFlow() }

    private val _appLockEnabled: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK_ENABLED, DEFAULT_APP_LOCK_ENABLED))
    }
    open val appLockEnabled: StateFlow<Boolean> by lazy { _appLockEnabled.asStateFlow() }

    open fun isConfirmSaveOnExit(): Boolean =
        prefs.getBoolean(KEY_CONFIRM_SAVE_ON_EXIT, DEFAULT_CONFIRM_SAVE_ON_EXIT)

    open fun setConfirmSaveOnExit(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONFIRM_SAVE_ON_EXIT, enabled).apply()
        _confirmSaveOnExit.value = enabled
    }

    open fun isConfirmDeleteNote(): Boolean =
        prefs.getBoolean(KEY_CONFIRM_DELETE_NOTE, DEFAULT_CONFIRM_DELETE_NOTE)

    open fun setConfirmDeleteNote(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONFIRM_DELETE_NOTE, enabled).apply()
        _confirmDeleteNote.value = enabled
    }

    open fun getEditorFontSize(): FontSizePreset =
        FontSizePreset.fromKey(prefs.getString(KEY_EDITOR_FONT_SIZE, FontSizePreset.DEFAULT.key))

    open fun setEditorFontSize(preset: FontSizePreset) {
        prefs.edit().putString(KEY_EDITOR_FONT_SIZE, preset.key).apply()
        _editorFontSize.value = preset
    }

    open fun getCardFontSize(): FontSizePreset =
        FontSizePreset.fromKey(prefs.getString(KEY_CARD_FONT_SIZE, FontSizePreset.DEFAULT.key))

    open fun setCardFontSize(preset: FontSizePreset) {
        prefs.edit().putString(KEY_CARD_FONT_SIZE, preset.key).apply()
        _cardFontSize.value = preset
    }

    open fun isAppLockEnabled(): Boolean =
        prefs.getBoolean(KEY_APP_LOCK_ENABLED, DEFAULT_APP_LOCK_ENABLED)

    open fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
        _appLockEnabled.value = enabled
    }

    companion object {
        const val PREFS_NAME = "keepnc_user_settings"
        const val KEY_CONFIRM_SAVE_ON_EXIT = "confirm_save_on_exit"
        const val KEY_CONFIRM_DELETE_NOTE = "confirm_delete_note"
        const val KEY_EDITOR_FONT_SIZE = "editor_font_size"
        const val KEY_CARD_FONT_SIZE = "card_font_size"
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        const val DEFAULT_CONFIRM_SAVE_ON_EXIT = true
        const val DEFAULT_CONFIRM_DELETE_NOTE = true
        const val DEFAULT_APP_LOCK_ENABLED = false
    }
}
