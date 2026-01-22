package rs.clash.android.ui

import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import rs.clash.android.ui.components.TitleBar
import rs.clash.android.viewmodel.HomeViewModel
import uniffi.clash_android_ffi.MemoryResponse

@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(
	navigator: DestinationsNavigator,
	modifier: Modifier = Modifier,
	viewModel: HomeViewModel = viewModel(),
) {
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

	Scaffold(
		topBar = {
			TitleBar(
				title = stringResource(R.string.home_title),
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
		modifier = modifier.fillMaxSize(),
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		item { Spacer(modifier = Modifier.height(16.dp)) }

		item(key = "vpn") {
			Box(modifier = Modifier.padding(horizontal = 16.dp)) {
				StatsCard(
					title = stringResource(R.string.stat_vpn),
					value = if (isVpnRunning) stringResource(R.string.stat_vpn_running) else stringResource(R.string.stat_vpn_stopped),
					subtitle = if (isVpnRunning) stringResource(R.string.stat_vpn_hint_stop) else stringResource(R.string.stat_vpn_hint_start),
					containerColor = if (isVpnRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
					onClick = onVpnToggle,
				)
			}
		}

		item(key = "memory") {
			Box(modifier = Modifier.padding(horizontal = 16.dp)) {
				StatsCard(
					title = stringResource(R.string.stat_memory),
					value = memory?.let { formatSize(it.inuse) } ?: stringResource(R.string.not_available),
					subtitle =
						memory?.let { stringResource(R.string.stat_memory_limit, formatSize(it.oslimit)) }
							?: stringResource(R.string.refreshing),
				)
			}
		}

		item(key = "connections") {
			Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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
		}

		item(key = "bandwidth") {
			Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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
		}

		item { Spacer(modifier = Modifier.height(16.dp)) }
	}
}

@Composable
fun StatsCard(
	title: String,
	value: String,
	modifier: Modifier = Modifier,
	subtitle: String = "",
	containerColor: Color? = null,
	onClick: (() -> Unit)? = null,
) {
	Card(
		modifier =
			modifier
				.fillMaxWidth()
				.semantics {
					contentDescription = "$title: $value${if (subtitle.isNotEmpty()) ", $subtitle" else ""}"
				},
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
		colors =
			if (containerColor != null) {
				CardDefaults.cardColors(containerColor = containerColor)
			} else {
				CardDefaults.cardColors()
			},
		onClick = onClick ?: {},
	) {
		Column(
			modifier = Modifier.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(4.dp),
		) {
			Text(
				text = title,
				style = MaterialTheme.typography.labelLarge,
				color = MaterialTheme.colorScheme.secondary,
			)
			Text(
				text = value,
				style = MaterialTheme.typography.headlineMedium,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.primary,
			)
			if (subtitle.isNotEmpty()) {
				Text(
					text = subtitle,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.outline,
				)
			}
		}
	}
}
