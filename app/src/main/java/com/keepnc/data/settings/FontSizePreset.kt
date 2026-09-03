package com.keepnc.data.settings

import androidx.annotation.StringRes
import com.keepnc.R

/**
 * Font size presets for note text.
 *
 * Supports independent configuration for the editor and note cards.
 */
enum class FontSizePreset(
    val key: String,
    @StringRes val labelRes: Int,
    val editorContentSp: Float,
    val editorCheckboxDp: Float,
    val cardContentSp: Float,
    val cardTitleSp: Float
) {
    SMALL(
        key = "small",
        labelRes = R.string.font_size_small,
        editorContentSp = 14f,
        editorCheckboxDp = 14f,
        cardContentSp = 11f,
        cardTitleSp = 13f
    ),
    MEDIUM(
        key = "medium",
        labelRes = R.string.font_size_medium,
        editorContentSp = 16f,
        editorCheckboxDp = 16f,
        cardContentSp = 13f,
        cardTitleSp = 15f
    ),
    LARGE(
        key = "large",
        labelRes = R.string.font_size_large,
        editorContentSp = 18f,
        editorCheckboxDp = 18f,
        cardContentSp = 15f,
        cardTitleSp = 17f
    ),
    EXTRA_LARGE(
        key = "extra_large",
        labelRes = R.string.font_size_extra_large,
        editorContentSp = 20f,
        editorCheckboxDp = 20f,
        cardContentSp = 17f,
        cardTitleSp = 19f
    );

    companion object {
        val DEFAULT = MEDIUM

        fun fromKey(key: String?): FontSizePreset =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
