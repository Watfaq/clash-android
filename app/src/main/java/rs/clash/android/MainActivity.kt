package rs.clash.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import rs.clash.android.theme.ClashAndroidTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
	external fun javaInit()

	private val notificationPermissionLauncher =
		registerForActivityResult(
			ActivityResultContracts.RequestPermission(),
		) { isGranted: Boolean ->
			// 权限结果处理
			if (isGranted) {
				android.util.Log.i("clash", "Notification permission granted")
			} else {
				android.util.Log.i("clash", "Notification permission denied")
			}
		}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		System.loadLibrary("clash_android_ffi")
		javaInit()

		// Apply language preference
		applyLanguagePreference()

		// Request notification permission (Android 13+)
		requestNotificationPermission()

		// enableEdgeToEdge()
		setContent {
			ClashAndroidTheme {
				Surface(color = MaterialTheme.colorScheme.background) {
					ClashApp()
				}
			}
		}
	}

	private fun applyLanguagePreference() {
		val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
		val languagePreference = prefs.getString("language", "SYSTEM") ?: "SYSTEM"

		val locale =
			when (languagePreference) {
				"SIMPLIFIED_CHINESE" -> Locale.SIMPLIFIED_CHINESE
				"ENGLISH" -> Locale.ENGLISH
				else -> return // Use system default
			}

		val config = Configuration(resources.configuration)
		config.setLocale(locale)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			createConfigurationContext(config)
		}

		@Suppress("DEPRECATION")
		resources.updateConfiguration(config, resources.displayMetrics)
	}

	private fun requestNotificationPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			when {
				ContextCompat.checkSelfPermission(
					this,
					Manifest.permission.POST_NOTIFICATIONS,
				) == PackageManager.PERMISSION_GRANTED -> {
					// 已有权限
					android.util.Log.i("clash", "Notification permission already granted")
				}
				else -> {
					// 请求权限
					notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
				}
			}
		}
	}
}
