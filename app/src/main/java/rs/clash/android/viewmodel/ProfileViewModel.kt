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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import rs.clash.android.Global
import rs.clash.android.model.Profile
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
	
	private val gson = Gson()

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
				val type = object : TypeToken<List<Profile>>() {}.type
				val loadedProfiles: List<Profile> = gson.fromJson(profilesJson, type)
				profiles.addAll(loadedProfiles)
				
				// Update active profile
				activeProfile = profiles.firstOrNull { it.isActive }
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	private fun saveProfiles() {
		val profilesJson = gson.toJson(profiles)
		prefs.edit {
			putString("profiles_list", profilesJson)
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
			val newProfile =
				Profile(
					name = fileName,
					filePath = file.absolutePath,
					fileSize = fileSize,
					isActive = profiles.isEmpty(), // First profile becomes active
				)
			
			// If this is the first profile or user wants to activate it, deactivate others
			if (profiles.isEmpty()) {
				profiles.add(newProfile)
				activeProfile = newProfile
			} else {
				profiles.add(newProfile)
			}
			
			saveProfiles()

			// Save to SharedPreferences for backward compatibility
			prefs.edit {
				putString("profile_path", file.absolutePath)
			}

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
}
