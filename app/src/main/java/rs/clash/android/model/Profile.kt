package rs.clash.android.model

import java.util.UUID

data class Profile(
	val id: String = UUID.randomUUID().toString(),
	val name: String,
	val filePath: String,
	val createdAt: Long = System.currentTimeMillis(),
	val isActive: Boolean = false,
	val fileSize: Long = 0,
)
