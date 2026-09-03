package com.keepnc.data.settings

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsStorageTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var storage: SettingsStorage

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        storage = SettingsStorage(fakePrefs)
    }

    @Test
    fun `default values are true for confirm save and confirm delete`() {
        assertTrue(storage.isConfirmSaveOnExit())
        assertTrue(storage.isConfirmDeleteNote())
        assertEquals(true, storage.confirmSaveOnExit.value)
        assertEquals(true, storage.confirmDeleteNote.value)
    }

    @Test
    fun `setConfirmSaveOnExit updates preference and stateflow`() {
        storage.setConfirmSaveOnExit(false)

        assertFalse(storage.isConfirmSaveOnExit())
        assertEquals(false, storage.confirmSaveOnExit.value)
        assertFalse(fakePrefs.getBoolean(SettingsStorage.KEY_CONFIRM_SAVE_ON_EXIT, true))

        storage.setConfirmSaveOnExit(true)
        assertTrue(storage.isConfirmSaveOnExit())
        assertEquals(true, storage.confirmSaveOnExit.value)
        assertTrue(fakePrefs.getBoolean(SettingsStorage.KEY_CONFIRM_SAVE_ON_EXIT, false))
    }

    @Test
    fun `setConfirmDeleteNote updates preference and stateflow`() {
        storage.setConfirmDeleteNote(false)

        assertFalse(storage.isConfirmDeleteNote())
        assertEquals(false, storage.confirmDeleteNote.value)
        assertFalse(fakePrefs.getBoolean(SettingsStorage.KEY_CONFIRM_DELETE_NOTE, true))

        storage.setConfirmDeleteNote(true)
        assertTrue(storage.isConfirmDeleteNote())
        assertEquals(true, storage.confirmDeleteNote.value)
        assertTrue(fakePrefs.getBoolean(SettingsStorage.KEY_CONFIRM_DELETE_NOTE, false))
    }

    @Test
    fun `default values for font sizes are MEDIUM`() {
        assertEquals(FontSizePreset.MEDIUM, storage.getEditorFontSize())
        assertEquals(FontSizePreset.MEDIUM, storage.getCardFontSize())
        assertEquals(FontSizePreset.MEDIUM, storage.editorFontSize.value)
        assertEquals(FontSizePreset.MEDIUM, storage.cardFontSize.value)
    }

    @Test
    fun `setEditorFontSize updates preference and stateflow`() {
        storage.setEditorFontSize(FontSizePreset.LARGE)

        assertEquals(FontSizePreset.LARGE, storage.getEditorFontSize())
        assertEquals(FontSizePreset.LARGE, storage.editorFontSize.value)
        assertEquals(FontSizePreset.LARGE.key, fakePrefs.getString(SettingsStorage.KEY_EDITOR_FONT_SIZE, null))

        storage.setEditorFontSize(FontSizePreset.SMALL)
        assertEquals(FontSizePreset.SMALL, storage.getEditorFontSize())
        assertEquals(FontSizePreset.SMALL, storage.editorFontSize.value)
    }

    @Test
    fun `setCardFontSize updates preference and stateflow`() {
        storage.setCardFontSize(FontSizePreset.EXTRA_LARGE)

        assertEquals(FontSizePreset.EXTRA_LARGE, storage.getCardFontSize())
        assertEquals(FontSizePreset.EXTRA_LARGE, storage.cardFontSize.value)
        assertEquals(FontSizePreset.EXTRA_LARGE.key, fakePrefs.getString(SettingsStorage.KEY_CARD_FONT_SIZE, null))

        storage.setCardFontSize(FontSizePreset.SMALL)
        assertEquals(FontSizePreset.SMALL, storage.getCardFontSize())
        assertEquals(FontSizePreset.SMALL, storage.cardFontSize.value)
    }

    @Test
    fun `default value for appLockEnabled is false`() {
        assertFalse(storage.isAppLockEnabled())
        assertEquals(false, storage.appLockEnabled.value)
    }

    @Test
    fun `setAppLockEnabled updates preference and stateflow`() {
        storage.setAppLockEnabled(true)

        assertTrue(storage.isAppLockEnabled())
        assertEquals(true, storage.appLockEnabled.value)
        assertTrue(fakePrefs.getBoolean(SettingsStorage.KEY_APP_LOCK_ENABLED, false))

        storage.setAppLockEnabled(false)
        assertFalse(storage.isAppLockEnabled())
        assertEquals(false, storage.appLockEnabled.value)
        assertFalse(fakePrefs.getBoolean(SettingsStorage.KEY_APP_LOCK_ENABLED, true))
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = values
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = values[key] as? Set<String> ?: defValues
        override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(values)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}

        private class FakeEditor(private val target: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clear = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { pending[key] = value }
            override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor = apply { pending[key] = values }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply { pending[key] = value }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply { pending[key] = value }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply { pending[key] = value }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply { pending[key] = value }
            override fun remove(key: String): SharedPreferences.Editor = apply { pending[key] = this }
            override fun clear(): SharedPreferences.Editor = apply { clear = true }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clear) target.clear()
                pending.forEach { (k, v) ->
                    if (v === this) target.remove(k) else target[k] = v
                }
                pending.clear()
            }
        }
    }
}
