package rs.clash.android.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rs.clash.android.Global
import uniffi.clash_android_ffi.ClashController
import uniffi.clash_android_ffi.Connection

class ConnectionsViewModel : ViewModel() {
	private val controller by lazy { ClashController("${Global.application.cacheDir}/clash.sock") }

	var connections by mutableStateOf<List<Connection>>(emptyList())
		private set

	var downloadTotal by mutableLongStateOf(0)
		private set

	var uploadTotal by mutableLongStateOf(0)
		private set

	var isRefreshing by mutableStateOf(false)
		private set

	var errorMessage by mutableStateOf<String?>(null)
		private set

	init {
		startPolling()
	}

	private fun startPolling() {
		viewModelScope.launch {
			while (true) {
				fetchConnections()
				delay(2000) // Poll every 2 seconds
			}
		}
	}

	fun fetchConnections() {
		isRefreshing = true
		errorMessage = null
		viewModelScope.launch {
			try {
				val response = controller.getConnections()
				connections = response.connections
				downloadTotal = response.downloadTotal
				uploadTotal = response.uploadTotal
			} catch (e: Exception) {
				Log.e("ConnectionsAPI", "Failed to fetch connections", e)
				errorMessage = "Failed to load connections: ${e.message}"
				connections = emptyList()
			} finally {
				isRefreshing = false
			}
		}
	}
}
