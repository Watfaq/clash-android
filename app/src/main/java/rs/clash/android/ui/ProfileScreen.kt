package rs.clash.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

@Destination<RootGraph>()
@OptIn(ExperimentalMaterial3Api::class)
@Composable
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

	Scaffold(
		modifier = modifier,
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.profile_title)) },
				windowInsets = WindowInsets(),
			)
		},
	) { padding ->
		Column(
			modifier =
				Modifier
					.fillMaxSize()
					.padding(padding)
					.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp),
		) {
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
}

@Composable
fun SelectedFileCard(
	fileName: String,
	fileSize: String,
	onClear: () -> Unit,
	modifier: Modifier = Modifier,
) {
	ElevatedCard(
		modifier = modifier,
		colors =
			CardDefaults.elevatedCardColors(
				containerColor = MaterialTheme.colorScheme.primaryContainer,
			),
	) {
		Row(
			modifier = Modifier.padding(16.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Icon(
				Icons.Default.Description,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.primary,
				modifier = Modifier.size(40.dp),
			)
			Spacer(modifier = Modifier.width(16.dp))
			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = stringResource(R.string.selected_file),
					style = MaterialTheme.typography.labelMedium,
					color = MaterialTheme.colorScheme.onPrimaryContainer,
				)
				Spacer(modifier = Modifier.height(4.dp))
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

@Composable
fun CurrentProfileCard(
	filePath: String?,
	modifier: Modifier = Modifier,
) {
	OutlinedCard(modifier = modifier) {
		Row(
			modifier = Modifier.padding(16.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			if (filePath != null) {
				Icon(
					Icons.Default.CheckCircle,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.primary,
					modifier = Modifier.size(40.dp),
				)
				Spacer(modifier = Modifier.width(16.dp))
				Column {
					Text(
						text = stringResource(R.string.current_profile),
						style = MaterialTheme.typography.labelMedium,
						color = MaterialTheme.colorScheme.primary,
					)
					Spacer(modifier = Modifier.height(4.dp))
					Text(
						text = stringResource(R.string.file_name),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
					Text(
						text = File(filePath).name,
						style = MaterialTheme.typography.bodyMedium,
						fontWeight = FontWeight.Medium,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
					)
					Spacer(modifier = Modifier.height(4.dp))
					Text(
						text = stringResource(R.string.file_path),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
					Text(
						text = filePath,
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis,
					)
				}
			} else {
				Icon(
					Icons.Default.FolderOpen,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.outline,
					modifier = Modifier.size(40.dp),
				)
				Spacer(modifier = Modifier.width(16.dp))
				Column {
					Text(
						text = stringResource(R.string.current_profile),
						style = MaterialTheme.typography.labelMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
					Spacer(modifier = Modifier.height(4.dp))
					Text(
						text = stringResource(R.string.no_profile),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
			}
		}
	}
}
