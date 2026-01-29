package rs.clash.android.model

import org.json.JSONObject
import java.util.UUID

data class Profile(
	val id: String = UUID.randomUUID().toString(),
	val name: String,
	val filePath: String,
	val createdAt: Long = System.currentTimeMillis(),
	val isActive: Boolean = false,
	val fileSize: Long = 0,
) {
	constructor(jsonObject: JSONObject) : this(
		id = jsonObject.getString("id"),
		name = jsonObject.getString("name"),
		filePath = jsonObject.getString("filePath"),
		createdAt = jsonObject.getLong("createdAt"),
		isActive = jsonObject.getBoolean("isActive"),
		fileSize = jsonObject.getLong("fileSize"),
	)

	fun asJsonObject(): JSONObject {
		val jsonObject = JSONObject()
		jsonObject.put("id", this.id)
		jsonObject.put("name", this.name)
		jsonObject.put("filePath", this.filePath)
		jsonObject.put("createdAt", this.createdAt)
		jsonObject.put("isActive", this.isActive)
		jsonObject.put("fileSize", this.fileSize)
		return jsonObject
	}
}
