package rs.clash.android.viewmodel

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import android.util.Log
import android.widget.Toast
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
import org.yaml.snakeyaml.Yaml
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import rs.clash.android.*
import rs.clash.android.service.TunService
import rs.clash.android.service.tunService
import java.io.File
import java.io.FileInputStream

class HomeViewModel : ViewModel() {
    var profilePath = MutableLiveData<String?>(null)
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
    var connectionCount by mutableIntStateOf(0)
        private set
    var totalDownload by mutableLongStateOf(0L)
        private set
    var totalUpload by mutableLongStateOf(0L)
        private set

    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "profile_path") {
            val path = sharedPreferences.getString("profile_path", null)
            profilePath.value = path
            Global.profilePath = path ?: ""
            updateClashApi()
        }
    }

    init {
        val context = Global.application.applicationContext
        val sharedPreferences = context.getSharedPreferences("file_prefs", MODE_PRIVATE)
        val initialPath = sharedPreferences.getString("profile_path", null)
        profilePath.value = initialPath
        Global.profilePath = initialPath ?: ""
        
        updateClashApi()

        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)

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
        val api = clashApi
        if (api == null || !isVpnRunning) return
        try {
            memoryUsage = api.getMemory()
            val connResponse = api.getConnections()
            connectionCount = connResponse.connections.size
            totalDownload = connResponse.downloadTotal
            totalUpload = connResponse.uploadTotal
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
            val httpClient = HttpClient(Android) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    })
                }
            }

            val ktorfit = Ktorfit.Builder()
                .baseUrl("http://$address/")
                .httpClient(httpClient)
                .build()

            // Use the recommended way to create API
            clashApi = ktorfit.createClashApi()
            if (isVpnRunning) {
                fetchProxies()
            }
        } catch (e: Exception) {
            Log.e("ClashAPI", "Failed to create Ktorfit", e)
        }
    }

    private fun resolveExternalController(path: String): String {
        return try {
            val file = File(path)
            if (!file.exists()) return "127.0.0.1:9090"
            
            val yaml = Yaml()
            val config = yaml.load<Map<String, Any>>(FileInputStream(file))
            val controller = config["external-controller"] as? String
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
        val api = clashApi
        if (api == null || !isVpnRunning) return
        
        isRefreshing = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val response = api.getProxies()
                proxies = response.proxies
                
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
        val api = clashApi ?: return
        try {
            delays[name] = "testing..."
            val response = api.getProxyDelay(name)
            delays[name] = "${response.delay}ms"
        } catch (e: Exception) {
            delays[name] = "timeout"
        }
    }

    fun selectProxy(groupName: String, proxyName: String) {
        val api = clashApi ?: return
        viewModelScope.launch {
            try {
                api.selectProxy(groupName, mapOf("name" to proxyName))
                fetchProxies()
            } catch (e: Exception) {
                Log.e("ClashAPI", "Failed to select proxy", e)
            }
        }
    }

    fun startVpn(launcher: ManagedActivityResultLauncher<Intent, ActivityResult>? = null) {
        val app = Global.application
        if (Global.profilePath.isEmpty()) {
            Toast.makeText(app, "Please select a config file first", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = VpnService.prepare(app)
        if (intent != null) {
            launcher?.launch(intent)
        } else {
            app.startService(TunService::class.intent)
        }
    }

    fun stopVpn() {
        tunService?.stopVpn()
    }
}
