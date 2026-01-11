package rs.clash.android.viewmodel

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.VpnService
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.getValue
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
import org.yaml.snakeyaml.Yaml
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import rs.clash.android.*
import rs.clash.android.service.TunService
import rs.clash.android.service.tunService
import java.io.File
import java.io.FileInputStream

class HomeViewModel : ViewModel() {
    var profilePath = MutableLiveData<String?>(null)
    var tunIntent: Intent? = null
    var isVpnRunning by mutableStateOf(tunService != null)
        private set

    var proxies by mutableStateOf<Map<String, Proxy>>(emptyMap())
        private set
    
    var isRefreshing by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var clashApi: ClashApi? = null
    val delays = mutableStateMapOf<String, String>()

    // Overview data
    var memoryUsage by mutableStateOf<MemoryResponse?>(null)
        private set
    var connectionCount by mutableStateOf(0)
        private set
    var totalDownload by mutableStateOf(0L)
        private set
    var totalUpload by mutableStateOf(0L)
        private set

    init {
        val context = Global.application.applicationContext
        val sharedPreferences = context.getSharedPreferences("file_prefs", MODE_PRIVATE)
        profilePath.value = sharedPreferences.getString("profile_path", null)
        Global.profilePath = profilePath.value ?: ""
        
        updateClashApi()

        sharedPreferences.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == "profile_path") {
                profilePath.value = sharedPreferences.getString("profile_path", null)
                Global.profilePath = profilePath.value ?: ""
                updateClashApi()
            }
        }

        viewModelScope.launch {
            Global.isServiceRunning.collectLatest { running ->
                isVpnRunning = running
                if (running) {
                    delay(1000)
                    fetchProxies()
                    startStatsPolling()
                } else {
                    proxies = emptyMap()
                    delays.clear()
                    errorMessage = null
                    memoryUsage = null
                    connectionCount = 0
                    totalDownload = 0L
                    totalUpload = 0L
                }
            }
        }
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
        if (clashApi == null || !isVpnRunning) return
        try {
            memoryUsage = clashApi?.getMemory()
            val connResponse = clashApi?.getConnections()
            connectionCount = connResponse?.connections?.size ?: 0
            totalDownload = connResponse?.downloadTotal ?: 0L
            totalUpload = connResponse?.uploadTotal ?: 0L
        } catch (e: Exception) {
            Log.e("ClashAPI", "Failed to fetch stats", e)
        }
    }

    private fun updateClashApi() {
        val path = profilePath.value
        val address = if (!path.isNullOrEmpty()) {
            resolveExternalController(path)
        } else {
            "127.0.0.1:9090"
        }

        try {
            val retrofit = Retrofit.Builder()
                .baseUrl("http://$address/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            clashApi = retrofit.create(ClashApi::class.java)
            if (isVpnRunning) {
                fetchProxies()
            }
        } catch (e: Exception) {
            Log.e("ClashAPI", "Failed to create Retrofit", e)
        }
    }

    private fun resolveExternalController(path: String): String {
        return try {
            val file = File(path)
            if (!file.exists()) return "127.0.0.1:9090"
            
            val yaml = Yaml()
            val config = yaml.load<Map<String, Any>>(FileInputStream(file))
            var controller = config["external-controller"] as? String
            if (controller.isNullOrEmpty()) {
                "127.0.0.1:9090"
            } else {
                if (controller.startsWith(":")) {
                    "127.0.0.1$controller"
                } else if (controller.startsWith("0.0.0.0")) {
                    controller.replace("0.0.0.0", "127.0.0.1")
                } else {
                    controller
                }
            }
        } catch (e: Exception) {
            "127.0.0.1:9090"
        }
    }

    fun fetchProxies() {
        if (clashApi == null || !isVpnRunning) return
        
        isRefreshing = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val response = clashApi?.getProxies()
                proxies = response?.proxies ?: emptyMap()
                
                proxies.forEach { (name, proxy) ->
                    val lastDelay = proxy.history?.lastOrNull()?.delay
                    if (lastDelay != null && lastDelay > 0) {
                        delays[name] = "${lastDelay}ms"
                    }
                }
            } catch (e: Exception) {
                Log.e("ClashAPI", "Failed to fetch proxies", e)
                errorMessage = "API Error: ${e.message}"
            } finally {
                isRefreshing = false
            }
        }
    }

    fun testGroupDelay(proxyNames: List<String>) {
        viewModelScope.launch {
            proxyNames.map { name ->
                async {
                    testProxyDelay(name)
                }
            }.awaitAll()
        }
    }

    suspend fun testProxyDelay(name: String) {
        try {
            delays[name] = "testing..."
            val response = clashApi?.getProxyDelay(name)
            if (response != null) {
                delays[name] = "${response.delay}ms"
            } else {
                delays[name] = "error"
            }
        } catch (e: Exception) {
            delays[name] = "timeout"
        }
    }

    fun selectProxy(groupName: String, proxyName: String) {
        viewModelScope.launch {
            try {
                clashApi?.selectProxy(groupName, mapOf("name" to proxyName))
                fetchProxies()
            } catch (e: Exception) {
                Log.e("ClashAPI", "Failed to select proxy", e)
            }
        }
    }

    fun startVpn(launcher: ManagedActivityResultLauncher<Intent, ActivityResult>) {
        val app = Global.application
        tunIntent = VpnService.prepare(app)
        if (tunIntent != null) {
            launcher.launch(tunIntent!!)
        } else {
            tunIntent = TunService::class.intent
            app.startService(tunIntent!!)
        }
    }

    fun stopVpn() {
        tunService?.stopVpn()
    }
}
