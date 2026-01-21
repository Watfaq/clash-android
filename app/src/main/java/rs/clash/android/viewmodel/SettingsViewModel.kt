package rs.clash.android.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

enum class DarkModePreference {
	SYSTEM,
	LIGHT,
	DARK,
}

enum class LanguagePreference {
	SYSTEM,
	SIMPLIFIED_CHINESE,
	ENGLISH,
}

class SettingsViewModel(
	application: Application,
) : AndroidViewModel(application) {
	private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

	var darkModePreference: DarkModePreference by mutableStateOf(loadDarkModePreference())
		private set

	var languagePreference: LanguagePreference by mutableStateOf(loadLanguagePreference())
		private set

	private fun loadDarkModePreference(): DarkModePreference {
		val value = prefs.getString("dark_mode", "SYSTEM") ?: "SYSTEM"
		return try {
			DarkModePreference.valueOf(value)
		} catch (e: IllegalArgumentException) {
			DarkModePreference.SYSTEM
		}
	}

	private fun loadLanguagePreference(): LanguagePreference {
		val value = prefs.getString("language", "SYSTEM") ?: "SYSTEM"
		return try {
			LanguagePreference.valueOf(value)
		} catch (e: IllegalArgumentException) {
			LanguagePreference.SYSTEM
		}
	}

	fun updateDarkModePreference(preference: DarkModePreference) {
		darkModePreference = preference
		prefs.edit().putString("dark_mode", preference.name).apply()
	}

	fun updateLanguagePreference(preference: LanguagePreference) {
		languagePreference = preference
		prefs.edit().putString("language", preference.name).apply()
	}

	fun getDarkModeDisplayName(): String =
		when (darkModePreference) {
			DarkModePreference.SYSTEM -> "跟随系统"
			DarkModePreference.LIGHT -> "浅色"
			DarkModePreference.DARK -> "深色"
		}

	fun getLanguageDisplayName(): String =
		when (languagePreference) {
			LanguagePreference.SYSTEM -> "跟随系统"
			LanguagePreference.SIMPLIFIED_CHINESE -> "简体中文"
			LanguagePreference.ENGLISH -> "English"
		}
}
