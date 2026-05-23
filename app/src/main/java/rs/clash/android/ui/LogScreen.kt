package rs.clash.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import rs.clash.android.viewmodel.LogViewModel
import uniffi.clash_android_ffi.LogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Terminal color palette
private val bgDark = Color(0xFF1E1E1E)
private val fgDefault = Color(0xFFD4D4D4)
private val colorError = Color(0xFFF44747)
private val colorWarn = Color(0xFFCCA700)
private val colorInfo = Color(0xFF4EC9B0)
private val colorDebug = Color(0xFF9CDCFE)
private val colorTrace = Color(0xFF808080)
private val colorTimestamp = Color(0xFF6A9955)
private val colorLevelBg = Color(0xFF2D2D2D)

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
	viewModel: LogViewModel = viewModel(),
) {
	val logs = viewModel.logs
	val isAutoScroll = viewModel.isAutoScroll
	val listState = rememberLazyListState()
	val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

	LaunchedEffect(Unit) {
		viewModel.startPolling()
	}

	// Auto-scroll to bottom when new logs arrive
	LaunchedEffect(logs.size, isAutoScroll) {
		if (isAutoScroll && logs.isNotEmpty()) {
			listState.animateScrollToItem(logs.size - 1)
		}
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Logs") },
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = MaterialTheme.colorScheme.surface,
				),
				actions = {
					IconButton(onClick = { viewModel.toggleAutoScroll() }) {
						Icon(
							Icons.Default.VerticalAlignBottom,
							contentDescription = if (isAutoScroll) "Auto-scroll ON" else "Auto-scroll OFF",
							tint = if (isAutoScroll) {
								MaterialTheme.colorScheme.primary
							} else {
								MaterialTheme.colorScheme.onSurfaceVariant
							},
						)
					}
					IconButton(onClick = { viewModel.clearLogs() }) {
						Icon(
							Icons.Default.DeleteSweep,
							contentDescription = "Clear logs",
						)
					}
				},
			)
		},
	) { padding ->
		if (logs.isEmpty()) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(padding)
					.background(bgDark),
			) {
				Text(
					text = "No logs yet. Start the VPN to see clash-rs logs.",
					color = colorTrace,
					fontFamily = FontFamily.Monospace,
					fontSize = 13.sp,
					modifier = Modifier.padding(16.dp),
				)
			}
		} else {
			LazyColumn(
				state = listState,
				modifier = Modifier
					.fillMaxSize()
					.padding(padding)
					.background(bgDark),
			) {
				items(
					items = logs,
					key = { "${it.timestamp}_${it.message.hashCode()}_${System.identityHashCode(it)}" },
				) { entry ->
					LogEntryRow(entry, dateFormat)
				}
			}
		}
	}
}

@Composable
private fun LogEntryRow(
	entry: LogEntry,
	dateFormat: SimpleDateFormat,
) {
	val levelColor = when (entry.level.uppercase()) {
		"ERROR" -> colorError
		"WARNING" -> colorWarn
		"WARN" -> colorWarn
		"INFO" -> colorInfo
		"DEBUG" -> colorDebug
		"TRACE" -> colorTrace
		else -> fgDefault
	}

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 8.dp, vertical = 1.dp)
			.horizontalScroll(rememberScrollState()),
	) {
		// Timestamp
		val timeStr = dateFormat.format(Date(entry.timestamp))
		Text(
			text = timeStr,
			color = colorTimestamp,
			fontFamily = FontFamily.Monospace,
			fontSize = 11.sp,
		)
		Spacer(Modifier.width(6.dp))

		// Level badge
		Text(
			text = entry.level.padEnd(5).take(5),
			color = levelColor,
			fontFamily = FontFamily.Monospace,
			fontSize = 11.sp,
			fontWeight = FontWeight.Bold,
			modifier = Modifier
				.background(colorLevelBg)
				.padding(horizontal = 3.dp),
		)
		Spacer(Modifier.width(6.dp))

		// Message
		Text(
			text = entry.message,
			color = fgDefault,
			fontFamily = FontFamily.Monospace,
			fontSize = 11.sp,
		)
	}
}
