package rs.clash.android

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import de.jensklingenberg.ktorfit.http.*

@Serializable
data class ProxiesResponse(val proxies: Map<String, Proxy>)

@Serializable
data class Proxy(
    val name: String,
    val type: String,
    val all: List<String>? = null,
    val now: String? = null,
    val history: List<DelayHistory>? = null
)

@Serializable
data class DelayHistory(
    val time: String,
    val delay: Int
)

@Serializable
data class DelayResponse(
    val delay: Int
)

@Serializable
data class MemoryResponse(val inuse: Long, val oslimit: Long)

@Serializable
data class ConnectionsResponse(
    val downloadTotal: Long,
    val uploadTotal: Long,
    val connections: List<Connection>
)

@Serializable
data class Connection(
    val id: String,
    val metadata: Metadata,
    val upload: Long,
    val download: Long,
    val start: String,
    val chains: List<String>,
    val rule: String
)

@Serializable
data class Metadata(
    val network: String,
    val type: String,
    val sourceIP: String,
    val destinationIP: String,
    val destinationPort: String,
    val host: String
)

@Serializable
data class ConfigResponse(
    @SerialName("external-controller") val externalController: String? = null,
    @SerialName("secret") val secret: String? = null,
    @SerialName("mode") val mode: String? = null
)

@Serializable
data class ProxySelect(@SerialName("name") val name: String? = null)

interface ClashApi {
    @GET("proxies")
    suspend fun getProxies(): ProxiesResponse

    @PUT("proxies/{name}")
    @Headers("Content-Type: application/json")
    suspend fun selectProxy(@Path("name") groupName: String, @Body proxy: ProxySelect)

    @GET("proxies/{name}/delay")
    suspend fun getProxyDelay(
        @Path("name") name: String,
        @Query("url") url: String = "http://www.gstatic.com/generate_204",
        @Query("timeout") timeout: Int = 5000
    ): DelayResponse

    @GET("memory")
    suspend fun getMemory(): MemoryResponse

    @GET("connections")
    suspend fun getConnections(): ConnectionsResponse

    @GET("configs")
    suspend fun getConfigs(): ConfigResponse

    @PATCH("configs")
    suspend fun updateConfig(@Body body: Map<String, String>)
}
