package rs.clash.android.theme

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
	darkColorScheme(
		primary = Purple80,
		secondary = PurpleGrey80,
		tertiary = Pink80,
	)

private val LightColorScheme =
	lightColorScheme(
		primary = Purple40,
		secondary = PurpleGrey40,
		tertiary = Pink40,
	/* Other default colors to override
	background = Color(0xFFFFFBFE),
	surface = Color(0xFFFFFBFE),
	onPrimary = Color.White,
	onSecondary = Color.White,
	onTertiary = Color.White,
	onBackground = Color(0xFF1C1B1F),
	onSurface = Color(0xFF1C1B1F),
	 */
	)

@Composable
fun ClashAndroidTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	// Dynamic color is available on Android 12+
	dynamicColor: Boolean = true,
	content: @Composable () -> Unit,
) {
	val context = LocalContext.current
	val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

	// Use state to make theme reactive to preference changes
	var darkModePreference by remember {
		mutableStateOf(prefs.getString("dark_mode", "SYSTEM") ?: "SYSTEM")
	}

	// Listen for preference changes
	DisposableEffect(prefs) {
		val listener =
			SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
				if (key == "dark_mode") {
					darkModePreference = prefs.getString("dark_mode", "SYSTEM") ?: "SYSTEM"
				}
			}
		prefs.registerOnSharedPreferenceChangeListener(listener)

		onDispose {
			prefs.unregisterOnSharedPreferenceChangeListener(listener)
		}
	}

	// Determine if dark theme should be used based on user preference
	val useDarkTheme =
		when (darkModePreference) {
			"LIGHT" -> false
			"DARK" -> true
			else -> darkTheme // SYSTEM or default
		}

	val colorScheme =
		when {
			dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
				if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
			}

			useDarkTheme -> DarkColorScheme
			else -> LightColorScheme
		}

	MaterialTheme(
		colorScheme = colorScheme,
		typography = Typography,
		content = content,
	)
}
