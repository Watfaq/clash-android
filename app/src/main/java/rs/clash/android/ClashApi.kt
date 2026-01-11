package rs.clash.android

import com.google.gson.annotations.SerializedName
import retrofit2.http.*

data class ProxiesResponse(val proxies: Map<String, Proxy>)

data class Proxy(
    val name: String,
    val type: String,
    val all: List<String>?,
    val now: String?,
    val history: List<DelayHistory>?
)

data class DelayHistory(
    val time: String,
    val delay: Int
)

data class DelayResponse(
    val delay: Int
)

data class MemoryResponse(val inuse: Long, val oslimit: Long)

data class ConnectionsResponse(
    val downloadTotal: Long,
    val uploadTotal: Long,
    val connections: List<Connection>
)

data class Connection(
    val id: String,
    val metadata: Metadata,
    val upload: Long,
    val download: Long,
    val start: String,
    val chains: List<String>,
    val rule: String
)

data class Metadata(
    val network: String,
    val type: String,
    val sourceIP: String,
    val destinationIP: String,
    val destinationPort: String,
    val host: String
)

data class ConfigResponse(
    @SerializedName("external-controller") val externalController: String?,
    @SerializedName("secret") val secret: String?,
    @SerializedName("mode") val mode: String?
)

interface ClashApi {
    @GET("proxies")
    suspend fun getProxies(): ProxiesResponse

    @PUT("proxies/{name}")
    suspend fun selectProxy(@Path("name") groupName: String, @Body body: Map<String, String>)

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
