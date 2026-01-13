package rs.clash.android.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rs.clash.android.Global
import java.io.File
import java.io.RandomAccessFile

class LogsViewModel : ViewModel() {
    // 改为 List<AnnotatedString> 以支持 LazyColumn 列表渲染，提升超大日志下的性能
    var logLines by mutableStateOf<List<AnnotatedString>>(emptyList())
        private set

    init {
        startLogPolling()
    }

    private fun startLogPolling() {
        viewModelScope.launch {
            val logFile = File("${Global.application.cacheDir}/clash-rs.log")
            while (true) {
                if (logFile.exists()) {
                    val rawLogs = withContext(Dispatchers.IO) {
                        try {
                            // 读取最后 64KB，避免大文件内存溢出
                            readLastBytes(logFile, 65536)
                        } catch (e: Exception) {
                            "Error reading logs: ${e.message}"
                        }
                    }

                    val annotatedLines = formatLogsToLines(rawLogs)
                    
                    // 只有当内容变化时才更新，避免频繁重组
                    if (annotatedLines.size != logLines.size || 
                        (annotatedLines.isNotEmpty() && logLines.isNotEmpty() && annotatedLines.last().text != logLines.last().text)) {
                        logLines = annotatedLines
                    }
                } else {
                    logLines = listOf(AnnotatedString("No logs found at ${logFile.absolutePath}"))
                }
                delay(2000)
            }
        }
    }

    private fun readLastBytes(file: File, bytesToRead: Long): String {
        if (!file.exists() || file.length() == 0L) return ""
        return RandomAccessFile(file, "r").use { raf ->
            val fileLength = raf.length()
            val startPointer = (fileLength - bytesToRead).coerceAtLeast(0L)
            raf.seek(startPointer)
            val bytes = ByteArray((fileLength - startPointer).toInt())
            raf.readFully(bytes)
            val content = String(bytes, Charsets.UTF_8)
            if (startPointer > 0) content.substringAfter('\n') else content
        }
    }

    private fun formatLogsToLines(rawLogs: String): List<AnnotatedString> {
        return rawLogs.lines().filter { it.isNotBlank() }.map { line ->
            buildAnnotatedString {
                val color = when {
                    line.contains("ERROR", ignoreCase = true) -> Color(0xFFEF5350)
                    line.contains("WARN", ignoreCase = true) -> Color(0xFFFFA726)
                    line.contains("INFO", ignoreCase = true) -> Color(0xFF66BB6A)
                    line.contains("DEBUG", ignoreCase = true) -> Color(0xFF42A5F5)
                    else -> null
                }

                if (color != null) {
                    withStyle(style = SpanStyle(color = color)) {
                        append(line)
                    }
                } else {
                    append(line)
                }
            }
        }
    }
}
