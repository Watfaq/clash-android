package rs.clash.android.model

import org.json.JSONObject
import java.util.UUID

enum class ProfileType {
	LOCAL,
	REMOTE,
}

data class Profile(
	val id: String = UUID.randomUUID().toString(),
	val name: String,
	val filePath: String,
	val createdAt: Long = System.currentTimeMillis(),
	val isActive: Boolean = false,
	val fileSize: Long = 0,
	val type: ProfileType = ProfileType.LOCAL,
	val url: String? = null,
	val lastUpdated: Long? = null,
	val autoUpdate: Boolean = false,
	val userAgent: String? = null,
	val proxyUrl: String? = null,
) {
	constructor(jsonObject: JSONObject) : this(
		id = jsonObject.getString("id"),
		name = jsonObject.getString("name"),
		filePath = jsonObject.getString("filePath"),
		createdAt = jsonObject.getLong("createdAt"),
		isActive = jsonObject.getBoolean("isActive"),
		fileSize = jsonObject.getLong("fileSize"),
		type = ProfileType.valueOf(jsonObject.optString("type", "LOCAL")),
		url = jsonObject.optString("url").takeIf { it.isNotBlank() },
		lastUpdated = if (jsonObject.has("lastUpdated")) jsonObject.getLong("lastUpdated") else null,
		autoUpdate = jsonObject.optBoolean("autoUpdate", false),
		userAgent = jsonObject.optString("userAgent").takeIf { it.isNotBlank() },
		proxyUrl = jsonObject.optString("proxyUrl").takeIf { it.isNotBlank() },
	)

	fun asJsonObject(): JSONObject {
		val jsonObject = JSONObject()
		jsonObject.put("id", this.id)
		jsonObject.put("name", this.name)
		jsonObject.put("filePath", this.filePath)
		jsonObject.put("createdAt", this.createdAt)
		jsonObject.put("isActive", this.isActive)
		jsonObject.put("fileSize", this.fileSize)
		jsonObject.put("type", this.type.name)
		if (this.url != null) jsonObject.put("url", this.url)
		if (this.lastUpdated != null) jsonObject.put("lastUpdated", this.lastUpdated)
		jsonObject.put("autoUpdate", this.autoUpdate)
		if (this.userAgent != null) jsonObject.put("userAgent", this.userAgent)
		if (this.proxyUrl != null) jsonObject.put("proxyUrl", this.proxyUrl)
		return jsonObject
	}
}
