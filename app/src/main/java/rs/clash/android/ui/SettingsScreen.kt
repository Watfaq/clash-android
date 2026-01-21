package rs.clash.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import rs.clash.android.R
import rs.clash.android.viewmodel.DarkModePreference
import rs.clash.android.viewmodel.LanguagePreference
import rs.clash.android.viewmodel.SettingsViewModel

@Destination<RootGraph>
@Composable
fun SettingsScreen(
	navigator: DestinationsNavigator,
	modifier: Modifier = Modifier,
	viewModel: SettingsViewModel = viewModel(),
) {
	var showDarkModeDialog by remember { mutableStateOf(false) }
	var showLanguageDialog by remember { mutableStateOf(false) }

	Scaffold(
		topBar = {
			TitleBar(title = stringResource(R.string.settings_title))
		},
	) { padding ->
		LazyColumn(
			modifier =
				modifier
					.fillMaxSize()
					.padding(padding)
					.padding(horizontal = 16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			item { Spacer(modifier = Modifier.height(4.dp)) }

			// Appearance Section
			item {
				SectionHeader(text = stringResource(R.string.settings_appearance))
			}

			item {
				SettingsCard {
					SettingsItem(
						icon = Icons.Default.DarkMode,
						title = stringResource(R.string.settings_dark_mode),
						subtitle = viewModel.getDarkModeDisplayName(),
						onClick = {
							showDarkModeDialog = true
						},
					)
					HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
					SettingsItem(
						icon = Icons.Default.Language,
						title = stringResource(R.string.settings_language),
						subtitle = viewModel.getLanguageDisplayName(),
						onClick = {
							showLanguageDialog = true
						},
					)
				}
			}

			// About Section
			item {
				SectionHeader(text = stringResource(R.string.settings_about))
			}

			item {
				SettingsCard {
					SettingsItem(
						icon = Icons.Default.Info,
						title = stringResource(R.string.settings_version),
						subtitle = "1.0.0",
						onClick = {},
						showChevron = false,
					)
				}
			}

			item { Spacer(modifier = Modifier.height(16.dp)) }
		}

		// Dark Mode Dialog
		if (showDarkModeDialog) {
			DarkModeDialog(
				currentPreference = viewModel.darkModePreference,
				onDismiss = { showDarkModeDialog = false },
				onConfirm = { preference ->
					viewModel.updateDarkModePreference(preference)
					showDarkModeDialog = false
				},
			)
		}

		// Language Dialog
		if (showLanguageDialog) {
			LanguageDialog(
				currentPreference = viewModel.languagePreference,
				onDismiss = { showLanguageDialog = false },
				onConfirm = { preference ->
					viewModel.updateLanguagePreference(preference)
					showLanguageDialog = false
				},
			)
		}
	}
}

@Composable
private fun SectionHeader(
	text: String,
	modifier: Modifier = Modifier,
) {
	Text(
		text = text,
		style = MaterialTheme.typography.labelLarge,
		color = MaterialTheme.colorScheme.primary,
		fontWeight = FontWeight.SemiBold,
		modifier = modifier.padding(vertical = 4.dp),
	)
}

@Composable
private fun SettingsCard(
	modifier: Modifier = Modifier,
	content: @Composable () -> Unit,
) {
	Card(
		modifier = modifier.fillMaxWidth(),
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
	) {
		content()
	}
}

@Composable
private fun SettingsItem(
	icon: ImageVector,
	title: String,
	subtitle: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	showChevron: Boolean = true,
) {
	Row(
		modifier =
			modifier
				.fillMaxWidth()
				.clickable(onClick = onClick)
				.padding(16.dp),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.CenterVertically,
	) {
		Row(
			modifier = Modifier.weight(1f),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(16.dp),
		) {
			Icon(
				imageVector = icon,
				contentDescription = title,
				tint = MaterialTheme.colorScheme.primary,
			)
			Column {
				Text(
					text = title,
					style = MaterialTheme.typography.bodyLarge,
					fontWeight = FontWeight.Medium,
				)
				Text(
					text = subtitle,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}
		if (showChevron) {
			Icon(
				imageVector = Icons.Default.ChevronRight,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
	}
}

@Composable
private fun DarkModeDialog(
	currentPreference: DarkModePreference,
	onDismiss: () -> Unit,
	onConfirm: (DarkModePreference) -> Unit,
) {
	var selectedPreference by remember { mutableStateOf(currentPreference) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(R.string.settings_dark_mode)) },
		text = {
			Column(modifier = Modifier.selectableGroup()) {
				DarkModeOption(
					text = stringResource(R.string.dark_mode_system),
					selected = selectedPreference == DarkModePreference.SYSTEM,
					onClick = { selectedPreference = DarkModePreference.SYSTEM },
				)
				DarkModeOption(
					text = stringResource(R.string.dark_mode_light),
					selected = selectedPreference == DarkModePreference.LIGHT,
					onClick = { selectedPreference = DarkModePreference.LIGHT },
				)
				DarkModeOption(
					text = stringResource(R.string.dark_mode_dark),
					selected = selectedPreference == DarkModePreference.DARK,
					onClick = { selectedPreference = DarkModePreference.DARK },
				)
			}
		},
		confirmButton = {
			TextButton(onClick = { onConfirm(selectedPreference) }) {
				Text(stringResource(android.R.string.ok))
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text(stringResource(android.R.string.cancel))
			}
		},
	)
}

@Composable
private fun DarkModeOption(
	text: String,
	selected: Boolean,
	onClick: () -> Unit,
) {
	Row(
		modifier =
			Modifier
				.fillMaxWidth()
				.selectable(
					selected = selected,
					onClick = onClick,
					role = Role.RadioButton,
				).padding(vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		RadioButton(
			selected = selected,
			onClick = null,
		)
		Text(
			text = text,
			modifier = Modifier.padding(start = 16.dp),
			style = MaterialTheme.typography.bodyLarge,
		)
	}
}

@Composable
private fun LanguageDialog(
	currentPreference: LanguagePreference,
	onDismiss: () -> Unit,
	onConfirm: (LanguagePreference) -> Unit,
) {
	var selectedPreference by remember { mutableStateOf(currentPreference) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(R.string.settings_language)) },
		text = {
			Column(modifier = Modifier.selectableGroup()) {
				LanguageOption(
					text = "跟随系统",
					selected = selectedPreference == LanguagePreference.SYSTEM,
					onClick = { selectedPreference = LanguagePreference.SYSTEM },
				)
				LanguageOption(
					text = "简体中文",
					selected = selectedPreference == LanguagePreference.SIMPLIFIED_CHINESE,
					onClick = { selectedPreference = LanguagePreference.SIMPLIFIED_CHINESE },
				)
				LanguageOption(
					text = "English",
					selected = selectedPreference == LanguagePreference.ENGLISH,
					onClick = { selectedPreference = LanguagePreference.ENGLISH },
				)
			}
		},
		confirmButton = {
			TextButton(onClick = { onConfirm(selectedPreference) }) {
				Text(stringResource(android.R.string.ok))
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text(stringResource(android.R.string.cancel))
			}
		},
	)
}

@Composable
private fun LanguageOption(
	text: String,
	selected: Boolean,
	onClick: () -> Unit,
) {
	Row(
		modifier =
			Modifier
				.fillMaxWidth()
				.selectable(
					selected = selected,
					onClick = onClick,
					role = Role.RadioButton,
				).padding(vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		RadioButton(
			selected = selected,
			onClick = null,
		)
		Text(
			text = text,
			modifier = Modifier.padding(start = 16.dp),
			style = MaterialTheme.typography.bodyLarge,
		)
	}
}
