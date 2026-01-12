package rs.clash.android.viewmodel

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.net.LocalSocket
import android.net.LocalSocketAddress
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
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import rs.clash.android.*
import rs.clash.android.service.TunService
import rs.clash.android.service.tunService
import java.io.File
import java.net.InetAddress
import java.net.Socket

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
        val socketPath = File("/data/user/0/rs.clash.android/cache/clash.sock").absolutePath

        try {
            val okHttpClient = OkHttpClient.Builder()
                .socketFactory(object : javax.net.SocketFactory() {
                    override fun createSocket(): Socket {
                        return object : Socket() {
                            val localSocket = LocalSocket()
                            override fun connect(endpoint: java.net.SocketAddress?, timeout: Int) {
                                localSocket.connect(LocalSocketAddress(socketPath, LocalSocketAddress.Namespace.FILESYSTEM))
                            }
                            override fun getInputStream() = localSocket.inputStream
                            override fun getOutputStream() = localSocket.outputStream
                            override fun isConnected() = localSocket.isConnected
                            override fun close() = localSocket.close()
                        }
                    }
                    override fun createSocket(host: String?, port: Int) = createSocket()
                    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int) = createSocket()
                    override fun createSocket(host: InetAddress?, port: Int) = createSocket()
                    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int) = createSocket()
                })
                .build()

            val httpClient = HttpClient(OkHttp) {
                engine {
                    preconfigured = okHttpClient
                }
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    })
                }
            }

            val ktorfit = Ktorfit.Builder()
                .baseUrl("http://localhost/")
                .httpClient(httpClient)
                .build()

            clashApi = ktorfit.createClashApi()
            if (isVpnRunning) {
                fetchProxies()
            }
        } catch (e: Exception) {
            Log.e("ClashAPI", "Failed to create Ktorfit", e)
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
