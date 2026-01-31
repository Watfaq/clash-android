package rs.clash.android.viewmodel

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import rs.clash.android.Global
import rs.clash.android.service.TunService
import rs.clash.android.service.tunService
import rs.clash.android.ui.snackbar.SnackbarController.Companion.showMessage
import uniffi.clash_android_ffi.ClashController
import uniffi.clash_android_ffi.EyreException
import uniffi.clash_android_ffi.MemoryResponse
import uniffi.clash_android_ffi.Proxy
import uniffi.clash_android_ffi.formatEyreError
import uniffi.clash_android_ffi.shutdown

class HomeViewModel : ViewModel() {
	var profilePath = MutableLiveData<String?>(null)
	var isVpnRunning by mutableStateOf(tunService != null)
		private set

	var proxies by mutableStateOf<Array<Proxy>>(emptyArray())
		private set

	private val controller by lazy { ClashController("${Global.application.cacheDir}/clash.sock") }
	var isRefreshing by mutableStateOf(false)
		private set

	val delays = mutableStateMapOf<String, String>()

	// Overview data
	var memoryUsage by mutableStateOf<MemoryResponse?>(null)
		private set
	var connectionCount by mutableIntStateOf(0)
		private set
	var totalDownload by mutableLongStateOf(0)
		private set
	var totalUpload by mutableLongStateOf(0)
		private set

	private val sharedPreferenceChangeListener =
		SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
			if (key == "profile_path") {
				val path = sharedPreferences.getString("profile_path", null)
				profilePath.value = path
				Global.profilePath = path ?: ""
			}
		}

	init {
		val context = Global.application.applicationContext
		val sharedPreferences = context.getSharedPreferences("file_prefs", MODE_PRIVATE)
		val initialPath = sharedPreferences.getString("profile_path", null)
		profilePath.value = initialPath
		Global.profilePath = initialPath ?: ""

		sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)

		viewModelScope.launch {
			Global.isServiceRunning.collectLatest { running ->
				isVpnRunning = running
				if (running) {
					delay(1000)
					fetchProxies()
					startStatsPolling()
				} else {
					proxies = emptyArray()
					delays.clear()
					memoryUsage = null
					connectionCount = 0
					totalDownload = 0
					totalUpload = 0
				}
			}
		}
	}

	override fun onCleared() {
		super.onCleared()
		val sharedPreferences = Global.application.getSharedPreferences("file_prefs", MODE_PRIVATE)
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
	}

	private fun startStatsPolling() {
		viewModelScope.launch {
			while (isVpnRunning) {
				fetchOverviewStats()
				delay(3000) // Poll every 3 seconds
			}
		}
	}

	private suspend fun fetchOverviewStats() {
		if (!isVpnRunning) return
		try {
			memoryUsage = controller.getMemory()
			val connResponse = controller.getConnections()
			connectionCount = connResponse.connections.size
			totalDownload = connResponse.downloadTotal
			totalUpload = connResponse.uploadTotal
		} catch (e: EyreException) {
			showMessage("Failed to fetch stats ${formatEyreError(e)}" )
		}
	}

	fun fetchProxies() {
		if (!isVpnRunning) return

		isRefreshing = true
		viewModelScope.launch {
			try {
				val proxies = controller.getProxies()

				proxies.forEach { proxy ->
					val lastDelay = proxy.history.lastOrNull()?.delay
					if (lastDelay != null && lastDelay > 0) {
						delays[proxy.name] = "${lastDelay}ms"
					}
				}
				this@HomeViewModel.proxies = proxies.toTypedArray()
			} catch (e: EyreException) {
				showMessage("API Error: ${formatEyreError(e)}")
			} finally {
				isRefreshing = false
			}
		}
	}

	fun testGroupDelay(proxyNames: List<String>) {
		viewModelScope.launch {
			proxyNames
				.map { name ->
					async {
						testProxyDelay(name)
					}
				}.awaitAll()
		}
	}

	suspend fun testProxyDelay(name: String) {
		try {
			delays[name] = "testing..."
			val response = controller.getProxyDelay(name, null, null)
			delays[name] = "${response.delay}ms"
		} catch (e: Exception) {
			delays[name] = "timeout"
		}
	}

	fun selectProxy(
		groupName: String,
		proxyName: String,
	) {
		viewModelScope.launch {
			try {
				controller.selectProxy(groupName, proxyName)
				fetchProxies()
			} catch (e: EyreException) {
				showMessage("Failed to select proxy ${e.message}")
			}
		}
	}

	fun startVpn(launcher: ManagedActivityResultLauncher<Intent, ActivityResult>? = null) {
		val app = Global.application
		if (Global.profilePath.isEmpty()) {
			showMessage("Please select a config file first")
			return
		}
		val intent = VpnService.prepare(app)
		if (intent != null) {
			launcher?.launch(intent)
		} else {
			app.startService(Intent(app, TunService::class.java))
		}
	}

	fun stopVpn() {
		shutdown()
		tunService?.stopVpn()
	}
}
