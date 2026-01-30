package rs.clash.android.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import rs.clash.android.Global
import rs.clash.android.model.Profile
import rs.clash.android.model.ProfileType
import uniffi.clash_android_ffi.EyreException
import uniffi.clash_android_ffi.formatEyreError
import uniffi.clash_android_ffi.verifyConfig
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

data class FileInfo(
	val name: String,
	val uri: Uri,
	val size: Long = 0,
)

class ProfileViewModel : ViewModel() {
	private val prefs = Global.application.getSharedPreferences("file_prefs", Context.MODE_PRIVATE)
	var selectedFile by mutableStateOf<FileInfo?>(null)
		private set

	var isImporting by mutableStateOf(false)
		private set

	var savedFilePath by mutableStateOf<String?>(null)
		private set

	var isVerifying by mutableStateOf(false)
		private set

	var verificationResult by mutableStateOf<String?>(null)
		private set

	// Multiple profiles support
	val profiles = mutableStateListOf<Profile>()
	
	var activeProfile by mutableStateOf<Profile?>(null)
		private set

	fun selectFile(
		context: Context,
		uri: Uri,
	) {
		val cursor = context.contentResolver.query(uri, null, null, null, null)
		var fileName = "config.yaml"
		var fileSize = 0L

		cursor?.use {
			if (it.moveToFirst()) {
				val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
				val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
				if (nameIndex != -1) {
					fileName = it.getString(nameIndex)
				}
				if (sizeIndex != -1) {
					fileSize = it.getLong(sizeIndex)
				}
			}
		}

		selectedFile = FileInfo(fileName, uri, fileSize)
	}

	fun clearSelection() {
		selectedFile = null
	}

	fun loadSavedFilePath() {
		savedFilePath = prefs.getString("profile_path", null)
		// Load profiles list
		loadProfiles()
	}

	private fun loadProfiles() {
		val profilesJson = prefs.getString("profiles_list", null)
		
		profiles.clear()
		if (profilesJson != null) {
			try {
				val jsonArray = JSONArray(profilesJson)
				val loadedProfiles = mutableListOf<Profile>()
				
				for (i in 0 until jsonArray.length()) {
					val jsonObject = jsonArray.getJSONObject(i)
					val profile = Profile(jsonObject)
					loadedProfiles.add(profile)
				}
				
				profiles.addAll(loadedProfiles)
				
				// Update active profile
				activeProfile = profiles.firstOrNull { it.isActive }
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	private fun saveProfiles() {
		val jsonArray = JSONArray()
		profiles.forEach { profile ->
			jsonArray.put(profile.asJsonObject())
		}
		
		prefs.edit {
			putString("profiles_list", jsonArray.toString())
		}
	}

	fun saveFileToAppDirectory(
		context: Context,
		uri: Uri,
		profileName: String? = null,
	): String? {
		isImporting = true
		return try {
			val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
			val fileName =
				profileName ?: selectedFile?.name?.substringBeforeLast('.') ?: "profile_${System.currentTimeMillis()}"
			
			// Create unique file name
			val file = File(context.filesDir, fileName)
			if (file.exists()) {
				file.delete()
			}
			file.createNewFile()
			
			var fileSize = 0L
			inputStream?.use { input ->
				FileOutputStream(file).use { output ->
					fileSize = input.copyTo(output)
				}
			}

			savedFilePath = file.absolutePath

			// Add to profiles list
			val isFirstProfile = profiles.isEmpty()
			val newProfile =
				Profile(
					name = fileName,
					filePath = file.absolutePath,
					fileSize = fileSize,
					isActive = isFirstProfile, // Only first profile becomes active
				)
			
			profiles.add(newProfile)
			
			// If this is the first profile, set it as active
			if (isFirstProfile) {
				activeProfile = newProfile
				// Update SharedPreferences for active profile
				prefs.edit {
					putString("profile_path", file.absolutePath)
				}
			}
			
			saveProfiles()

			Toast.makeText(context, "配置文件导入成功", Toast.LENGTH_SHORT).show()
			file.absolutePath
		} catch (e: Exception) {
			Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
			null
		} finally {
			isImporting = false
		}
	}

	fun activateProfile(
		context: Context,
		profile: Profile,
	) {
		// Deactivate all profiles
		val updatedProfiles = profiles.map { it.copy(isActive = false) }
		profiles.clear()
		profiles.addAll(updatedProfiles)
		
		// Activate selected profile
		val index = profiles.indexOfFirst { it.id == profile.id }
		if (index >= 0) {
			profiles[index] = profiles[index].copy(isActive = true)
			activeProfile = profiles[index]
			savedFilePath = profiles[index].filePath
			
			// Update SharedPreferences
			prefs.edit {
				putString("profile_path", profiles[index].filePath)
			}
			
			saveProfiles()
			Toast.makeText(context, "已切换到配置: ${profile.name}", Toast.LENGTH_SHORT).show()
		}
	}

	fun deleteProfile(
		context: Context,
		profile: Profile,
	) {
		val file = File(profile.filePath)
		if (file.exists()) {
			file.delete()
		}
		
		profiles.removeAll { it.id == profile.id }
		
		// If deleted profile was active, activate the first remaining profile
		if (profile.isActive && profiles.isNotEmpty()) {
			activateProfile(context, profiles[0])
		} else if (profiles.isEmpty()) {
			activeProfile = null
			savedFilePath = null
			prefs.edit {
				remove("profile_path")
			}
		}
		
		saveProfiles()
		Toast.makeText(context, "配置已删除: ${profile.name}", Toast.LENGTH_SHORT).show()
	}

	fun renameProfile(
		context: Context,
		profile: Profile,
		newName: String,
	) {
		val index = profiles.indexOfFirst { it.id == profile.id }
		if (index >= 0) {
			profiles[index] = profiles[index].copy(name = newName)
			if (profiles[index].isActive) {
				activeProfile = profiles[index]
			}
			saveProfiles()
			Toast.makeText(context, "配置已重命名", Toast.LENGTH_SHORT).show()
		}
	}

	fun formatFileSize(size: Long): String {
		if (size <= 0) return "0 B"
		val units = arrayOf("B", "KB", "MB", "GB")
		val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
		return String.format(Locale.US, "%.1f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
	}

	fun verify(path: String): Pair<Boolean, String> =
		try {
			true to verifyConfig(path)
		} catch (e: EyreException) {
			false to formatEyreError(e)
		}

	fun verifyCurrentConfig(context: Context) {
		if (savedFilePath == null) {
			verificationResult = "未找到配置文件"
			return
		}

		isVerifying = true
		verificationResult = null

		try {
			val (isValid, content) = verify(savedFilePath!!)
			verificationResult =
				if (isValid) {
					"配置文件合法\n\n$content"
				} else {
					"配置文件不合法：$content"
				}
		} catch (e: Exception) {
			verificationResult = "验证失败: ${e.message}"
		} finally {
			isVerifying = false
		}
	}

	fun clearVerificationResult() {
		verificationResult = null
	}

	// Remote profile download
	var isDownloading by mutableStateOf(false)
		private set

	fun addRemoteProfile(
		context: Context,
		profileName: String,
		url: String,
		autoUpdate: Boolean = false,
		userAgent: String? = null,
		proxyUrl: String? = null,
	) {
		viewModelScope.launch {
			isDownloading = true
			try {
				// Create unique file name based on profile name
				val fileName = profileName.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]"), "_")
				val file = File(context.filesDir, fileName)
				
				// Download config from URL using Rust FFI
				withContext(Dispatchers.IO) {
					val result =
						uniffi.clash_android_ffi.downloadConfigFromUrl(
							url,
							file.absolutePath,
							userAgent,
							proxyUrl,
						)
					
					if (!result.success) {
						withContext(Dispatchers.Main) {
							Toast
								.makeText(
									context,
									"下载失败: ${result.errorMessage ?: "未知错误"}",
									Toast.LENGTH_LONG,
								).show()
						}
						return@withContext
					}
					
					// Verify the downloaded config
					val (isValid, _) = verify(file.absolutePath)
					if (!isValid) {
						file.delete()
						withContext(Dispatchers.Main) {
							Toast
								.makeText(
									context,
									"配置文件验证失败，已删除",
									Toast.LENGTH_LONG,
								).show()
						}
						return@withContext
					}
					
					withContext(Dispatchers.Main) {
						// Add to profiles list
						val isFirstProfile = profiles.isEmpty()
						val newProfile =
							Profile(
								name = profileName,
								filePath = file.absolutePath,
								fileSize = result.fileSize.toLong(),
								isActive = isFirstProfile,
								type = ProfileType.REMOTE,
								url = url,
								lastUpdated = System.currentTimeMillis(),
								autoUpdate = autoUpdate,
								userAgent = userAgent,
								proxyUrl = proxyUrl,
							)
						saveProfiles()
					}
				}
			} catch (e: Exception) {
				withContext(Dispatchers.Main) {
					Toast.makeText(context, "添加失败: ${e.message}", Toast.LENGTH_LONG).show()
				}
			} finally {
				isDownloading = false
			}
		}
	}

	fun updateRemoteProfile(
		context: Context,
		profile: Profile,
		userAgent: String? = null,
		proxyUrl: String? = null,
	) {
		if (profile.type != ProfileType.REMOTE || profile.url == null) {
			Toast.makeText(context, "只能更新远程配置", Toast.LENGTH_SHORT).show()
			return
		}
		
		viewModelScope.launch {
			isDownloading = true
			try {
				withContext(Dispatchers.IO) {
					val file = File(profile.filePath)
					// Use provided parameters or fall back to profile's stored values
					val effectiveUserAgent = userAgent ?: profile.userAgent
					val effectiveProxyUrl = proxyUrl ?: profile.proxyUrl
					val result =
						uniffi.clash_android_ffi.downloadConfigFromUrl(
							profile.url,
							file.absolutePath,
							effectiveUserAgent,
							effectiveProxyUrl,
						)
					
					if (!result.success) {
						withContext(Dispatchers.Main) {
							Toast
								.makeText(
									context,
									"更新失败: ${result.errorMessage ?: "未知错误"}",
									Toast.LENGTH_LONG,
								).show()
						}
						return@withContext
					}
					
					// Verify the downloaded config
					val (isValid, _) = verify(file.absolutePath)
					if (!isValid) {
						withContext(Dispatchers.Main) {
							Toast.makeText(context, "配置文件验证失败", Toast.LENGTH_LONG).show()
						}
						return@withContext
					}
					
					withContext(Dispatchers.Main) {
						// Update profile
						val index = profiles.indexOfFirst { it.id == profile.id }
						if (index >= 0) {
							profiles[index] =
								profiles[index].copy(
									fileSize = result.fileSize.toLong(),
									lastUpdated = System.currentTimeMillis(),
								)
							if (profiles[index].isActive) {
								activeProfile = profiles[index]
							}
							saveProfiles()
						}
						
						Toast.makeText(context, "配置更新成功", Toast.LENGTH_SHORT).show()
					}
				}
			} catch (e: Exception) {
				withContext(Dispatchers.Main) {
					Toast.makeText(context, "更新失败: ${e.message}", Toast.LENGTH_LONG).show()
				}
			} finally {
				isDownloading = false
			}
		}
	}
}
