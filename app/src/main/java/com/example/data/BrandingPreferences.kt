package com.example.data

import android.content.Context
import androidx.core.content.edit
import com.example.model.ThemePreset

data class CachedSchoolBranding(
    val schoolName: String = "My School",
    val logoBase64: String = "",
    val themePreset: ThemePreset = ThemePreset.BLUE_TEAL
)

class BrandingPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("my_school_branding_prefs", Context.MODE_PRIVATE)

    fun saveBranding(schoolName: String, logoBase64: String, themePreset: ThemePreset) {
        prefs.edit {
            putString(KEY_NAME, schoolName.ifBlank { "My School" })
                .putString(KEY_LOGO, logoBase64)
                .putString(KEY_THEME, themePreset.name)
        }
    }

    fun loadBranding(): CachedSchoolBranding {
        val name = prefs.getString(KEY_NAME, "My School") ?: "My School"
        val logo = prefs.getString(KEY_LOGO, "") ?: ""
        val themeStr = prefs.getString(KEY_THEME, ThemePreset.BLUE_TEAL.name) ?: ThemePreset.BLUE_TEAL.name
        val preset = runCatching { ThemePreset.valueOf(themeStr) }.getOrDefault(ThemePreset.BLUE_TEAL)
        return CachedSchoolBranding(schoolName = name, logoBase64 = logo, themePreset = preset)
    }

    companion object {
        private const val KEY_NAME = "school_name"
        private const val KEY_LOGO = "logo_base64"
        private const val KEY_THEME = "theme_preset"
    }
}
