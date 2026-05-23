package rs.clash.android.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uniffi.clash_android_ffi.LogEntry
import uniffi.clash_android_ffi.clearClashLogs
import uniffi.clash_android_ffi.getClashLogs

class LogViewModel : ViewModel() {
	var logs by mutableStateOf<List<LogEntry>>(emptyList())
		private set

	var isAutoScroll by mutableStateOf(true)
		private set

	private var pollingJob: Job? = null

	fun startPolling() {
		if (pollingJob?.isActive == true) return
		pollingJob = viewModelScope.launch {
			while (isActive) {
				logs = getClashLogs()
				delay(500) // Poll every 500ms
			}
		}
	}

	fun stopPolling() {
		pollingJob?.cancel()
		pollingJob = null
	}

	fun clearLogs() {
		clearClashLogs()
		logs = emptyList()
	}

	fun toggleAutoScroll() {
		isAutoScroll = !isAutoScroll
	}

	override fun onCleared() {
		stopPolling()
		super.onCleared()
	}
}
