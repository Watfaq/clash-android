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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import rs.clash.android.R
import rs.clash.android.viewmodel.HomeViewModel
import uniffi.clash_android_ffi.MemoryResponse
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

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
		floatingActionButton = {
			FloatingActionButton(
				onClick = {
					if (isVpnRunning) {
						viewModel.stopVpn()
					} else {
						viewModel.startVpn(vpnPermissionLauncher)
					}
				},
			) {
				val (icon, contentDescription) =
					if (isVpnRunning) {
						Icons.Filled.Stop to "Stop VPN"
					} else {
						Icons.Filled.PlayArrow to "Start VPN"
					}
				Icon(
					imageVector = icon,
					contentDescription = contentDescription,
				)
			}
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
	modifier: Modifier = Modifier,
) {
	LazyColumn(
		modifier = modifier.fillMaxSize(),
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		item { Spacer(modifier = Modifier.height(16.dp)) }

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
) {
	Card(
		modifier =
			modifier
				.fillMaxWidth()
				.semantics {
					contentDescription = "$title: $value${if (subtitle.isNotEmpty()) ", $subtitle" else ""}"
				},
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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

fun formatSize(size: Long): String {
	if (size <= 0) return "0 B"
	val units = arrayOf("B", "KB", "MB", "GB", "TB")
	val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
	return String.format(Locale.US, "%.2f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}
