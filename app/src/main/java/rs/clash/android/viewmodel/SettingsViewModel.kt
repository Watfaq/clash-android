package rs.clash.android.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import rs.clash.android.R

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

	var foregroundServiceEnabled: Boolean by mutableStateOf(loadForegroundServiceEnabled())
		private set

	var fakeIpEnabled: Boolean by mutableStateOf(loadFakeIpEnabled())
		private set

	var ipv6Enabled: Boolean by mutableStateOf(loadIpv6Enabled())
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

	private fun loadForegroundServiceEnabled(): Boolean = prefs.getBoolean("foreground_service_enabled", false)

	private fun loadFakeIpEnabled(): Boolean = prefs.getBoolean("fake_ip", false)

	private fun loadIpv6Enabled(): Boolean = prefs.getBoolean("ipv6", true)

	fun updateDarkModePreference(preference: DarkModePreference) {
		darkModePreference = preference
		prefs.edit { putString("dark_mode", preference.name) }
	}

	fun updateLanguagePreference(preference: LanguagePreference) {
		languagePreference = preference
		prefs.edit { putString("language", preference.name) }
	}

	fun updateForegroundServiceEnabled(enabled: Boolean) {
		foregroundServiceEnabled = enabled
		prefs.edit { putBoolean("foreground_service_enabled", enabled) }
	}

	fun updateFakeIpEnabled(enabled: Boolean) {
		fakeIpEnabled = enabled
		prefs.edit { putBoolean("fake_ip", enabled) }
	}

	fun updateIpv6Enabled(enabled: Boolean) {
		ipv6Enabled = enabled
		prefs.edit { putBoolean("ipv6", enabled) }
	}

	fun getDarkModeDisplayName(): String {
		val context = getApplication<Application>().applicationContext
		return when (darkModePreference) {
			DarkModePreference.SYSTEM -> context.getString(R.string.dark_mode_system)
			DarkModePreference.LIGHT -> context.getString(R.string.dark_mode_light)
			DarkModePreference.DARK -> context.getString(R.string.dark_mode_dark)
		}
	}

	fun getLanguageDisplayName(): String {
		val context = getApplication<Application>().applicationContext
		return when (languagePreference) {
			LanguagePreference.SYSTEM -> context.getString(R.string.language_system)
			LanguagePreference.SIMPLIFIED_CHINESE -> context.getString(R.string.language_simplified_chinese)
			LanguagePreference.ENGLISH -> context.getString(R.string.language_english)
		}
	}
}
