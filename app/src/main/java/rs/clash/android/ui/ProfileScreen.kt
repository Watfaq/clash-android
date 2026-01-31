package rs.clash.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import rs.clash.android.R
import rs.clash.android.model.Profile
import rs.clash.android.model.ProfileType
import rs.clash.android.ui.components.TextInfoDialog
import rs.clash.android.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

@Destination<RootGraph>()
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
	navigator: DestinationsNavigator,
	modifier: Modifier = Modifier,
	vm: ProfileViewModel = viewModel(),
) {
	val context = LocalContext.current
	val showNameDialog = remember { mutableStateOf(false) }
	val profileName = remember { mutableStateOf("") }
	val showRemoteDialog = remember { mutableStateOf(false) }
	val remoteName = remember { mutableStateOf("") }
	val remoteUrl = remember { mutableStateOf("") }
	val remoteAutoUpdate = remember { mutableStateOf(false) }
	val remoteUserAgent = remember { mutableStateOf("") }
	val remoteProxyUrl = remember { mutableStateOf("") }

	var showInfoDialog by remember { mutableStateOf(false) }
	var showErrorDetailsDialog by remember { mutableStateOf(false) }
	val snackbarHostState = remember { SnackbarHostState() }
	
	// Handle error display with Snackbar
	LaunchedEffect(vm.errorInfo) {
		vm.errorInfo?.let { error ->
			snackbarHostState.showSnackbar(
				message = error.message,
				actionLabel = "详情",
				duration = SnackbarDuration.Long
			).let { result ->
				if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
					showErrorDetailsDialog = true
				}
			}
		}
	}
	
	// Load saved file path on first composition
	LaunchedEffect(Unit) {
		vm.loadSavedFilePath()
	}

	val launcher =
		rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
			uri?.let {
				vm.selectFile(context, it)
				// Show dialog to enter profile name
				showNameDialog.value = true
			}
		}

	// Info dialog
	if (showInfoDialog) {
		TextInfoDialog(
			title = stringResource(R.string.about_title),
			content =
				"""
				已知问题:
				- 目前 clash-rs 并非100%兼容 mihomo 的配置文件, 应用配置文件前可先检验配置文件合法性。
				- 切换配置文件后需要重启应用。
				""".trimIndent(),
			onDismiss = { showInfoDialog = false },
		)
	}

	// Error details dialog
	if (showErrorDetailsDialog && vm.errorInfo != null) {
		AlertDialog(
			onDismissRequest = { 
				showErrorDetailsDialog = false
				vm.clearError()
			},
			icon = {
				Icon(
					imageVector = Icons.Filled.Error,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.error
				)
			},
			title = { Text("错误详情") },
			text = {
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.verticalScroll(rememberScrollState())
				) {
					Text(
						text = vm.errorInfo!!.fullMessage,
						style = MaterialTheme.typography.bodyMedium
					)
				}
			},
			confirmButton = {
				TextButton(onClick = { 
					showErrorDetailsDialog = false
					vm.clearError()
				}) {
					Text("确定")
				}
			}
		)
	}

	// Remote profile dialog
	if (showRemoteDialog.value) {
		AlertDialog(
			onDismissRequest = {
				showRemoteDialog.value = false
				remoteName.value = ""
				remoteUrl.value = ""
				remoteAutoUpdate.value = false
				remoteUserAgent.value = ""
				remoteProxyUrl.value = ""
			},
			title = { Text("添加远程配置") },
			text = {
				Column(
					verticalArrangement = Arrangement.spacedBy(8.dp),
					modifier = Modifier.verticalScroll(rememberScrollState()),
				) {
					OutlinedTextField(
						value = remoteName.value,
						onValueChange = { remoteName.value = it },
						label = { Text("配置名称") },
						placeholder = { Text("我的远程配置") },
						singleLine = true,
						modifier = Modifier.fillMaxWidth(),
					)
					OutlinedTextField(
						value = remoteUrl.value,
						onValueChange = { remoteUrl.value = it },
						label = { Text("订阅 URL") },
						placeholder = { Text("https://example.com/config.yaml") },
						singleLine = true,
						modifier = Modifier.fillMaxWidth(),
					)
					OutlinedTextField(
						value = remoteUserAgent.value,
						onValueChange = { remoteUserAgent.value = it },
						label = { Text("User-Agent (可选)") },
						placeholder = { Text("自定义浏览器标识") },
						singleLine = true,
						modifier = Modifier.fillMaxWidth(),
					)
					OutlinedTextField(
						value = remoteProxyUrl.value,
						onValueChange = { remoteProxyUrl.value = it },
						label = { Text("HTTP 代理 (可选)") },
						placeholder = { Text("http://proxy.example.com:8080") },
						singleLine = true,
						modifier = Modifier.fillMaxWidth(),
					)
					Row(
						verticalAlignment = Alignment.CenterVertically,
						modifier =
							Modifier.clickable {
								remoteAutoUpdate.value = !remoteAutoUpdate.value
							},
					) {
						Icon(
							if (remoteAutoUpdate.value) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
							contentDescription = null,
						)
						Spacer(modifier = Modifier.width(8.dp))
						Text("启用自动更新")
					}
				}
			},
			confirmButton = {
				TextButton(
					onClick = {
						if (remoteName.value.isNotBlank() && remoteUrl.value.isNotBlank()) {
							vm.addRemoteProfile(
								context,
								remoteName.value,
								remoteUrl.value,
								remoteAutoUpdate.value,
								remoteUserAgent.value.takeIf { it.isNotBlank() },
								remoteProxyUrl.value.takeIf { it.isNotBlank() },
							)
							showRemoteDialog.value = false
							remoteName.value = ""
							remoteUrl.value = ""
							remoteAutoUpdate.value = false
							remoteUserAgent.value = ""
							remoteProxyUrl.value = ""
						}
					},
					enabled = remoteName.value.isNotBlank() && remoteUrl.value.isNotBlank() && !vm.isDownloading,
				) {
					if (vm.isDownloading) {
						CircularProgressIndicator(
							modifier = Modifier.size(16.dp),
							strokeWidth = 2.dp,
						)
					} else {
						Text("确定")
					}
				}
			},
			dismissButton = {
				TextButton(
					onClick = {
						showRemoteDialog.value = false
						remoteName.value = ""
						remoteUrl.value = ""
						remoteAutoUpdate.value = false
						remoteUserAgent.value = ""
						remoteProxyUrl.value = ""
					},
					enabled = !vm.isDownloading,
				) {
					Text("取消")
				}
			},
		)
	}

	// Name input dialog
	if (showNameDialog.value && vm.selectedFile != null) {
		AlertDialog(
			onDismissRequest = {
				showNameDialog.value = false
				profileName.value = ""
			},
			title = { Text("输入配置名称") },
			text = {
				OutlinedTextField(
					value = profileName.value,
					onValueChange = { profileName.value = it },
					label = { Text("配置名称") },
					placeholder = { Text(vm.selectedFile?.name ?: "我的配置") },
					singleLine = true,
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						val name =
							profileName.value.ifBlank {
								vm.selectedFile?.name?.substringBeforeLast('.') ?: "配置"
							}
						vm.saveFileToAppDirectory(context, vm.selectedFile!!.uri, name)
						vm.clearSelection()
						showNameDialog.value = false
						profileName.value = ""
					},
				) {
					Text("确定")
				}
			},
			dismissButton = {
				TextButton(
					onClick = {
						showNameDialog.value = false
						profileName.value = ""
						vm.clearSelection()
					},
				) {
					Text("取消")
				}
			},
		)
	}

	Scaffold(
		modifier = modifier,
		snackbarHost = {
			SnackbarHost(hostState = snackbarHostState) { data ->
				Snackbar(
					snackbarData = data,
					containerColor = MaterialTheme.colorScheme.errorContainer,
					contentColor = MaterialTheme.colorScheme.onErrorContainer,
					actionColor = MaterialTheme.colorScheme.error
				)
			}
		},
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.profile_title)) },
				windowInsets = WindowInsets(),
				actions = {
					IconButton(onClick = { showInfoDialog = true }) {
						Icon(
							imageVector = Icons.Filled.Info,
							contentDescription = stringResource(R.string.action_about),
						)
					}
				},
			)
		},
	) { padding ->
		Column(
			modifier =
				Modifier
					.fillMaxSize()
					.padding(padding)
					.padding(16.dp)
					.verticalScroll(rememberScrollState()),
			verticalArrangement = Arrangement.spacedBy(16.dp),
		) {
			// Current Active Profile Card
			if (vm.activeProfile != null) {
				ActiveProfileCard(
					profile = vm.activeProfile!!,
					onVerify = { vm.verifyCurrentConfig(context) },
					isVerifying = vm.isVerifying,
					modifier = Modifier.fillMaxWidth(),
				)
			} else {
				OutlinedCard(modifier = Modifier.fillMaxWidth()) {
					Column(
						modifier = Modifier.padding(16.dp),
						horizontalAlignment = Alignment.CenterHorizontally,
					) {
						Icon(
							Icons.Default.FolderOpen,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.outline,
							modifier = Modifier.size(48.dp),
						)
						Spacer(modifier = Modifier.height(8.dp))
						Text(
							text = "暂无活动配置",
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}
				}
			}

			// Profiles List
			if (vm.profiles.isNotEmpty()) {
				Text(
					text = "所有配置 (${vm.profiles.size})",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
				)
			}

			if (vm.verificationResult != null) {
				VerificationResultCard(
					result = vm.verificationResult!!,
					onDismiss = { vm.clearVerificationResult() },
					modifier = Modifier.fillMaxWidth(),
				)
			}

			Spacer(modifier = Modifier.height(8.dp))

			// Display all profiles
			vm.profiles.forEach { profile ->
				ProfileCard(
					profile = profile,
					onActivate = { vm.activateProfile(context, profile) },
					onDelete = { vm.deleteProfile(context, profile) },
					onRename = { newName -> vm.renameProfile(context, profile, newName) },
					onUpdate =
						if (profile.type == ProfileType.REMOTE) {
							{ vm.updateRemoteProfile(context, profile) }
						} else {
							null
						},
					modifier = Modifier.fillMaxWidth(),
				)
			}

			Spacer(modifier = Modifier.height(8.dp))

			// Action Buttons
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
			) {
				FilledTonalButton(
					onClick = {
						launcher.launch(arrayOf("*/*"))
					},
					modifier = Modifier.weight(1f),
				) {
					Icon(
						Icons.Default.FolderOpen,
						contentDescription = null,
						modifier = Modifier.size(20.dp),
					)
					Spacer(modifier = Modifier.width(8.dp))
					Text("本地配置")
				}
				
				FilledTonalButton(
					onClick = {
						showRemoteDialog.value = true
					},
					modifier = Modifier.weight(1f),
				) {
					Icon(
						Icons.Default.Refresh,
						contentDescription = null,
						modifier = Modifier.size(20.dp),
					)
					Spacer(modifier = Modifier.width(8.dp))
					Text("远程配置")
				}
			}
		}
	}
}

@Composable
fun ActiveProfileCard(
	profile: Profile,
	onVerify: () -> Unit,
	isVerifying: Boolean,
	modifier: Modifier = Modifier,
) {
	ElevatedCard(
		modifier = modifier,
		colors =
			CardDefaults.elevatedCardColors(
				containerColor = MaterialTheme.colorScheme.primaryContainer,
			),
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
			) {
				Icon(
					Icons.Default.CheckCircle,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.primary,
					modifier = Modifier.size(40.dp),
				)
				Spacer(modifier = Modifier.width(16.dp))
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = "当前活动配置",
						style = MaterialTheme.typography.labelMedium,
						color = MaterialTheme.colorScheme.primary,
					)
					Spacer(modifier = Modifier.height(4.dp))
					Text(
						text = profile.name,
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
					)
					Spacer(modifier = Modifier.height(2.dp))
					Text(
						text = formatFileSize(profile.fileSize),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
			}
			
			Spacer(modifier = Modifier.height(12.dp))
			
			FilledTonalButton(
				onClick = onVerify,
				modifier = Modifier.fillMaxWidth(),
				enabled = !isVerifying,
			) {
				if (isVerifying) {
					CircularProgressIndicator(
						modifier = Modifier.size(16.dp),
						color = MaterialTheme.colorScheme.onSecondaryContainer,
						strokeWidth = 2.dp,
					)
					Spacer(modifier = Modifier.width(8.dp))
					Text("验证中...")
				} else {
					Icon(
						Icons.Default.VerifiedUser,
						contentDescription = null,
						modifier = Modifier.size(16.dp),
					)
					Spacer(modifier = Modifier.width(8.dp))
					Text("验证配置")
				}
			}
		}
	}
}

@Composable
fun ProfileCard(
	profile: Profile,
	onActivate: () -> Unit,
	onDelete: () -> Unit,
	onRename: (String) -> Unit,
	onUpdate: (() -> Unit)? = null,
	modifier: Modifier = Modifier,
) {
	val showMenu = remember { mutableStateOf(false) }
	val showRenameDialog = remember { mutableStateOf(false) }
	val newName = remember { mutableStateOf(profile.name) }
	val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
	
	if (showRenameDialog.value) {
		AlertDialog(
			onDismissRequest = {
				showRenameDialog.value = false
				newName.value = profile.name
			},
			title = { Text("重命名配置") },
			text = {
				OutlinedTextField(
					value = newName.value,
					onValueChange = { newName.value = it },
					label = { Text("配置名称") },
					singleLine = true,
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						if (newName.value.isNotBlank()) {
							onRename(newName.value)
							showRenameDialog.value = false
						}
					},
				) {
					Text("确定")
				}
			},
			dismissButton = {
				TextButton(
					onClick = {
						showRenameDialog.value = false
						newName.value = profile.name
					},
				) {
					Text("取消")
				}
			},
		)
	}
	
	OutlinedCard(
		modifier = modifier.clickable(enabled = !profile.isActive) { onActivate() },
	) {
		Row(
			modifier = Modifier.padding(16.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Icon(
				if (profile.isActive) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
				contentDescription = if (profile.isActive) "Active" else "Inactive",
				tint = if (profile.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
				modifier = Modifier.size(24.dp),
			)
			Spacer(modifier = Modifier.width(16.dp))
			Column(modifier = Modifier.weight(1f)) {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Text(
						text = profile.name,
						style = MaterialTheme.typography.bodyLarge,
						fontWeight = if (profile.isActive) FontWeight.Bold else FontWeight.Normal,
					)
					if (profile.type == rs.clash.android.model.ProfileType.REMOTE) {
						Spacer(modifier = Modifier.width(4.dp))
						Icon(
							Icons.Default.Refresh,
							contentDescription = "Remote",
							tint = MaterialTheme.colorScheme.primary,
							modifier = Modifier.size(16.dp),
						)
					}
				}
				Spacer(modifier = Modifier.height(2.dp))
				Text(
					text = formatFileSize(profile.fileSize),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
				Text(
					text = dateFormat.format(Date(profile.createdAt)),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
				if (profile.type == rs.clash.android.model.ProfileType.REMOTE && profile.lastUpdated != null) {
					Text(
						text = "更新: ${dateFormat.format(Date(profile.lastUpdated))}",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.primary,
					)
				}
			}
			
			Box {
				IconButton(onClick = { showMenu.value = true }) {
					Icon(
						Icons.Default.MoreVert,
						contentDescription = "More options",
					)
				}
				
				DropdownMenu(
					expanded = showMenu.value,
					onDismissRequest = { showMenu.value = false },
				) {
					if (!profile.isActive) {
						DropdownMenuItem(
							text = { Text("设为活动配置") },
							onClick = {
								onActivate()
								showMenu.value = false
							},
							leadingIcon = {
								Icon(Icons.Default.CheckCircle, contentDescription = null)
							},
						)
					}
					if (profile.type == rs.clash.android.model.ProfileType.REMOTE && onUpdate != null) {
						DropdownMenuItem(
							text = { Text("更新配置") },
							onClick = {
								onUpdate()
								showMenu.value = false
							},
							leadingIcon = {
								Icon(Icons.Default.Refresh, contentDescription = null)
							},
						)
					}
					DropdownMenuItem(
						text = { Text("重命名") },
						onClick = {
							showRenameDialog.value = true
							showMenu.value = false
						},
						leadingIcon = {
							Icon(Icons.Default.Edit, contentDescription = null)
						},
					)
					DropdownMenuItem(
						text = { Text("删除") },
						onClick = {
							onDelete()
							showMenu.value = false
						},
						leadingIcon = {
							Icon(
								Icons.Default.Delete,
								contentDescription = null,
								tint = MaterialTheme.colorScheme.error,
							)
						},
					)
				}
			}
		}
	}
}

fun formatFileSize(size: Long): String {
	if (size <= 0) return "0 B"
	val units = arrayOf("B", "KB", "MB", "GB")
	val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
	return String.format(Locale.US, "%.1f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

@Composable
fun VerificationResultCard(
	result: String,
	onDismiss: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val isValid = result.contains("合法") || result.contains("valid")
	val scrollState = rememberScrollState()
	
	ElevatedCard(
		modifier = modifier,
		colors =
			CardDefaults.elevatedCardColors(
				containerColor =
					if (isValid) {
						MaterialTheme.colorScheme.tertiaryContainer
					} else {
						MaterialTheme.colorScheme.errorContainer
					},
			),
	) {
		Row(
			modifier = Modifier.padding(16.dp),
			verticalAlignment = Alignment.Top,
		) {
			Icon(
				if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
				contentDescription = null,
				tint =
					if (isValid) {
						MaterialTheme.colorScheme.tertiary
					} else {
						MaterialTheme.colorScheme.error
					},
				modifier = Modifier.size(24.dp),
			)
			Spacer(modifier = Modifier.width(12.dp))
			Column(
				modifier =
					Modifier
						.weight(1f)
						.height(300.dp)
						.verticalScroll(scrollState),
			) {
				Text(
					text = stringResource(R.string.validation_result),
					style = MaterialTheme.typography.labelMedium,
					color =
						if (isValid) {
							MaterialTheme.colorScheme.onTertiaryContainer
						} else {
							MaterialTheme.colorScheme.onErrorContainer
						},
				)
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = result,
					style = MaterialTheme.typography.bodySmall,
					color =
						if (isValid) {
							MaterialTheme.colorScheme.onTertiaryContainer
						} else {
							MaterialTheme.colorScheme.onErrorContainer
						},
					fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
				)
			}
			IconButton(
				onClick = onDismiss,
				modifier = Modifier.size(24.dp),
			) {
				Icon(
					Icons.Default.Close,
					contentDescription = "Close",
					tint =
						if (isValid) {
							MaterialTheme.colorScheme.onTertiaryContainer
						} else {
							MaterialTheme.colorScheme.onErrorContainer
						},
					modifier = Modifier.size(20.dp),
				)
			}
		}
	}
}
