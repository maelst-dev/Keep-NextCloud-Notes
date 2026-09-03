package com.keepnc.data.settings

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.keepnc.R

/**
 * Supported in-app languages for the application locale switcher.
 *
 * Designed to be easily extensible: to add a new language in the future:
 * 1. Add translation strings (e.g. res/values-de/strings.xml).
 * 2. Add locale tag (e.g. <locale android:name="de" />) in res/xml/locales_config.xml.
 * 3. Add an entry to this enum (e.g. GERMAN("de", R.string.language_german)).
 *
 * @param code BCP 47 language code, or empty string for system default.
 * @param labelRes Localized string resource describing the language.
 */
enum class AppLanguage(
    val code: String,
    @StringRes val labelRes: Int
) {
    SYSTEM("", R.string.language_system),
    ENGLISH("en", R.string.language_english),
    RUSSIAN("ru", R.string.language_russian);

    companion object {
        /** Finds [AppLanguage] matching the given language code, falling back to [SYSTEM]. */
        fun fromCode(code: String?): AppLanguage {
            if (code.isNullOrBlank()) return SYSTEM
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
        }

        /** Returns the currently active [AppLanguage] configured via [AppCompatDelegate]. */
        fun getCurrent(): AppLanguage {
            val locales = AppCompatDelegate.getApplicationLocales()
            if (locales.isEmpty) return SYSTEM
            val primaryLocale = locales[0] ?: return SYSTEM
            return fromCode(primaryLocale.language)
        }

        /** Applies the specified [AppLanguage] using [AppCompatDelegate.setApplicationLocales]. */
        fun apply(language: AppLanguage) {
            val localeList = if (language.code.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(language.code)
            }
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }
}
