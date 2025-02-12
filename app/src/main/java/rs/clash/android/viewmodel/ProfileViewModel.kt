package rs.clash.android.viewmodel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ProfileViewModel : ViewModel() {
    fun saveFileToAppDirectory(
        context: Context,
        uri: Uri,
    ): String? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "default")

        inputStream?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }

        Toast.makeText(context, "File saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        return file.absolutePath
    }
}
