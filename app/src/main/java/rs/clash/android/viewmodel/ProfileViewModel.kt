package rs.clash.android.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.log10
import kotlin.math.pow

data class FileInfo(
    val name: String,
    val uri: Uri,
    val size: Long = 0
)

class ProfileViewModel : ViewModel() {
    var selectedFile by mutableStateOf<FileInfo?>(null)
        private set

    var isImporting by mutableStateOf(false)
        private set

    var savedFilePath by mutableStateOf<String?>(null)
        private set

    fun selectFile(context: Context, uri: Uri) {
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

    fun loadSavedFilePath(context: Context) {
        val sharedPreferences = context.getSharedPreferences("file_prefs", Context.MODE_PRIVATE)
        savedFilePath = sharedPreferences.getString("profile_path", null)
    }

    fun saveFileToAppDirectory(
        context: Context,
        uri: Uri,
    ): String? {
        isImporting = true
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, "default")
            if (file.exists()){
                file.delete()
            }
            file.createNewFile()
            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            savedFilePath = file.absolutePath

            // Save to SharedPreferences
            val sharedPreferences = context.getSharedPreferences("file_prefs", Context.MODE_PRIVATE)
            sharedPreferences.edit {
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

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return "%.1f %s".format(size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }
}
