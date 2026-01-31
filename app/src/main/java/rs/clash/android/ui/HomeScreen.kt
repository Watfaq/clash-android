package rs.clash.android.ui

import android.app.Activity.RESULT_OK
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ConnectionsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import rs.clash.android.R
import rs.clash.android.formatSize
import rs.clash.android.ui.components.StatsCard
import rs.clash.android.ui.components.TextInfoDialog
import rs.clash.android.viewmodel.HomeViewModel
import uniffi.clash_android_ffi.MemoryResponse

@Destination<RootGraph>(start = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
	navigator: DestinationsNavigator,
	modifier: Modifier = Modifier,
	viewModel: HomeViewModel = viewModel(),
) {
	val context = LocalContext.current
	var showRestartDialog by remember { mutableStateOf(false) }
	var showInfoDialog by remember { mutableStateOf(false) }

	val vpnPermissionLauncher =
		rememberLauncherForActivityResult(
			contract = ActivityResultContracts.StartActivityForResult(),
			onResult = { result ->
				if (result.resultCode == RESULT_OK) {
					viewModel.startVpn()
				}
			},
		)

	val isVpnRunning = viewModel.isVpnRunning

	// Restart confirmation dialog
	if (showRestartDialog) {
		AlertDialog(
			onDismissRequest = { showRestartDialog = false },
			title = { Text(stringResource(R.string.restart_confirm_title)) },
			text = { Text(stringResource(R.string.restart_confirm_message)) },
			confirmButton = {
				TextButton(
					onClick = {
						showRestartDialog = false
						// Restart the app
						val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
						intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
						context.startActivity(intent)
						android.os.Process.killProcess(android.os.Process.myPid())
					},
				) {
					Text(stringResource(R.string.confirm))
				}
			},
			dismissButton = {
				TextButton(onClick = { showRestartDialog = false }) {
					Text(stringResource(R.string.cancel))
				}
			},
		)
	}

	// Info dialog
	if (showInfoDialog) {
		TextInfoDialog(
			title = stringResource(R.string.about_title),
			content =
				"""
				已知问题:
				- 目前 clash-rs 的优雅退出存在一些 bug, 为了正确性目前关闭 VPN 时会自动重启应用。
				""".trimIndent(),
			onDismiss = { showInfoDialog = false },
		)
	}

	Scaffold(
		modifier = Modifier.fillMaxSize(),
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.home_title)) },
				actions = {
					IconButton(onClick = { showInfoDialog = true }) {
						Icon(
							imageVector = Icons.Filled.Info,
							contentDescription = stringResource(R.string.action_about),
						)
					}
					IconButton(onClick = { showRestartDialog = true }) {
						Icon(
							imageVector = Icons.Filled.Refresh,
							contentDescription = stringResource(R.string.action_restart),
						)
					}
				},
				windowInsets = WindowInsets(),
			)
		},
	) { padding ->
		val memory by remember { derivedStateOf { viewModel.memoryUsage } }
		val connections by remember { derivedStateOf { viewModel.connectionCount } }
		val download by remember { derivedStateOf { viewModel.totalDownload } }
		val upload by remember { derivedStateOf { viewModel.totalUpload } }

		OverviewTab(
			memory = memory,
			connections = connections,
			download = download,
			upload = upload,
			isVpnRunning = isVpnRunning,
			onVpnToggle = {
				if (isVpnRunning) {
					viewModel.stopVpn()
					// Restart the app when VPN is stopped
					val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
					intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
					context.startActivity(intent)
					android.os.Process.killProcess(android.os.Process.myPid())
				} else {
					viewModel.startVpn(vpnPermissionLauncher)
				}
			},
			onConnectionsClick = {
				navigator.navigate(ConnectionsScreenDestination)
			},
			modifier = Modifier.padding(padding).fillMaxSize(),
		)
	}
}

@Composable
fun OverviewTab(
	memory: MemoryResponse?,
	connections: Int,
	download: Long,
	upload: Long,
	isVpnRunning: Boolean,
	onVpnToggle: () -> Unit,
	modifier: Modifier = Modifier,
	onConnectionsClick: () -> Unit = {},
) {
	LazyColumn(
		modifier =
			modifier
				.padding(horizontal = 16.dp)
				.fillMaxSize(),
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		item { Spacer(modifier = Modifier.height(16.dp)) }

		item(key = "vpn") {
			StatsCard(
				title = stringResource(R.string.stat_vpn),
				value = if (isVpnRunning) stringResource(R.string.stat_vpn_running) else stringResource(R.string.stat_vpn_stopped),
				subtitle = if (isVpnRunning) stringResource(R.string.stat_vpn_hint_stop) else stringResource(R.string.stat_vpn_hint_start),
				containerColor = if (isVpnRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
				onClick = onVpnToggle,
			)
		}

		item(key = "memory") {
			StatsCard(
				title = stringResource(R.string.stat_memory),
				value = memory?.let { formatSize(it.inuse) } ?: stringResource(R.string.not_available),
				subtitle =
					memory?.let { stringResource(R.string.stat_memory_limit, formatSize(it.oslimit)) }
						?: stringResource(R.string.refreshing),
			)
		}

		item(key = "connections") {
			StatsCard(
				title = stringResource(R.string.stat_connections),
				value = connections.toString(),
				subtitle =
					if (connections >
						0
					) {
						stringResource(R.string.stat_connections_ongoing)
					} else {
						stringResource(R.string.stat_connections_none)
					},
				onClick = onConnectionsClick,
			)
		}

		item(key = "bandwidth") {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(12.dp),
			) {
				StatsCard(
					title = stringResource(R.string.stat_download),
					value = formatSize(download),
					modifier = Modifier.weight(1f),
				)
				StatsCard(
					title = stringResource(R.string.stat_upload),
					value = formatSize(upload),
					modifier = Modifier.weight(1f),
				)
			}
		}

		item { Spacer(modifier = Modifier.height(16.dp)) }
	}
}
