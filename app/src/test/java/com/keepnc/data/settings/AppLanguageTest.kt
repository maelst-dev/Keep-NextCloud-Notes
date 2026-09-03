package com.keepnc.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `fromCode returns SYSTEM for empty or null or unknown codes`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode("   "))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode("unknown_code"))
    }

    @Test
    fun `fromCode returns matching language case-insensitively`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("EN"))
        assertEquals(AppLanguage.RUSSIAN, AppLanguage.fromCode("ru"))
        assertEquals(AppLanguage.RUSSIAN, AppLanguage.fromCode("RU"))
    }

    @Test
    fun `SYSTEM language has empty code`() {
        assertEquals("", AppLanguage.SYSTEM.code)
    }
}
