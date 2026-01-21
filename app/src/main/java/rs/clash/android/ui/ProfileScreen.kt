package rs.clash.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import rs.clash.android.R
import rs.clash.android.viewmodel.ProfileViewModel
import java.io.File

@Composable
@Destination<RootGraph>()
fun ProfileScreen(
	navigator: DestinationsNavigator,
	modifier: Modifier = Modifier,
	vm: ProfileViewModel = viewModel(),
) {
	val context = LocalContext.current

	// Load saved file path on first composition
	LaunchedEffect(Unit) {
		vm.loadSavedFilePath(context)
	}

	val launcher =
		rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
			uri?.let {
				vm.selectFile(context, it)
			}
		}

	Column(
		modifier =
			modifier
				.fillMaxSize()
				.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp),
	) {
		// Header
		Text(
			text = stringResource(R.string.profile_title),
			style = MaterialTheme.typography.headlineMedium,
			fontWeight = FontWeight.Bold,
		)

		Text(
			text = stringResource(R.string.profile_description),
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)

		Spacer(modifier = Modifier.height(8.dp))

		// Current Profile Card
		CurrentProfileCard(
			filePath = vm.savedFilePath,
			modifier = Modifier.fillMaxWidth(),
		)

		Spacer(modifier = Modifier.height(8.dp))

		// Selected File Card
		if (vm.selectedFile != null) {
			SelectedFileCard(
				fileName = vm.selectedFile!!.name,
				fileSize = vm.formatFileSize(vm.selectedFile!!.size),
				onClear = { vm.clearSelection() },
				modifier = Modifier.fillMaxWidth(),
			)
		}

		Spacer(modifier = Modifier.weight(1f))

		// Action Buttons
		Column(
			modifier = Modifier.fillMaxWidth(),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			if (vm.selectedFile != null) {
				Button(
					onClick = {
						vm.saveFileToAppDirectory(context, vm.selectedFile!!.uri)
						vm.clearSelection()
					},
					modifier = Modifier.fillMaxWidth(),
					enabled = !vm.isImporting,
				) {
					if (vm.isImporting) {
						CircularProgressIndicator(
							modifier = Modifier.size(20.dp),
							color = MaterialTheme.colorScheme.onPrimary,
							strokeWidth = 2.dp,
						)
						Spacer(modifier = Modifier.width(8.dp))
					} else {
						Icon(
							Icons.Default.FileUpload,
							contentDescription = null,
							modifier = Modifier.size(20.dp),
						)
						Spacer(modifier = Modifier.width(8.dp))
					}
					Text(stringResource(R.string.import_file))
				}
			}

			FilledTonalButton(
				onClick = {
					launcher.launch(arrayOf("*/*"))
				},
				modifier = Modifier.fillMaxWidth(),
			) {
				Icon(
					Icons.Default.FolderOpen,
					contentDescription = null,
					modifier = Modifier.size(20.dp),
				)
				Spacer(modifier = Modifier.width(8.dp))
				Text(stringResource(R.string.choose_file))
			}
		}
	}
}

@Composable
fun CurrentProfileCard(
	filePath: String?,
	modifier: Modifier = Modifier,
) {
	ElevatedCard(
		modifier = modifier,
		colors =
			CardDefaults.elevatedCardColors(
				containerColor = MaterialTheme.colorScheme.primaryContainer,
			),
	) {
		Column(
			modifier =
				Modifier
					.fillMaxWidth()
					.padding(16.dp),
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
			) {
				Icon(
					imageVector = if (filePath != null) Icons.Default.CheckCircle else Icons.Default.Description,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.onPrimaryContainer,
					modifier = Modifier.size(24.dp),
				)
				Spacer(modifier = Modifier.width(12.dp))
				Text(
					text = stringResource(R.string.current_profile),
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.SemiBold,
					color = MaterialTheme.colorScheme.onPrimaryContainer,
				)
			}

			Spacer(modifier = Modifier.height(8.dp))

			if (filePath != null) {
				val fileName = File(filePath).name
				Text(
					text = "${stringResource(R.string.file_name)}: $fileName",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onPrimaryContainer,
				)
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = "${stringResource(R.string.file_path)}: $filePath",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
				)
			} else {
				Text(
					text = stringResource(R.string.no_profile),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
					fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
				)
			}
		}
	}
}

@Composable
fun SelectedFileCard(
	fileName: String,
	fileSize: String,
	onClear: () -> Unit,
	modifier: Modifier = Modifier,
) {
	OutlinedCard(
		modifier = modifier,
		colors =
			CardDefaults.outlinedCardColors(
				containerColor = MaterialTheme.colorScheme.surfaceVariant,
			),
	) {
		Column(
			modifier =
				Modifier
					.fillMaxWidth()
					.padding(16.dp),
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically,
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
					modifier = Modifier.weight(1f),
				) {
					Icon(
						imageVector = Icons.Default.Description,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.primary,
						modifier = Modifier.size(24.dp),
					)
					Spacer(modifier = Modifier.width(12.dp))
					Column {
						Text(
							text = stringResource(R.string.selected_file),
							style = MaterialTheme.typography.labelMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
						Text(
							text = fileName,
							style = MaterialTheme.typography.bodyMedium,
							fontWeight = FontWeight.Medium,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis,
						)
						Text(
							text = fileSize,
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}
				}
			}
		}
	}
}
