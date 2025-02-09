package rs.clash.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.livedata.observeAsState

import rs.clash.android.theme.ClashAndroidTheme
import rs.clash.android.viewmodel.HomeViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : ComponentActivity() {

    var tunIntent: Intent? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Global.init(application)

//        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
//            .detectLeakedClosableObjects()
//            .build());

        enableEdgeToEdge()
        setContent {
            ClashAndroidTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    HomeScreen()
                }
            }
        }
    }
    @Composable
    fun HomeScreen(
        viewModel: HomeViewModel = viewModel()
    ) {
        var vpnStatus by remember { mutableStateOf("VPN Stopped") }
        val profilePath by viewModel.profilePath.observeAsState()

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = vpnStatus, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))

            Button(onClick = {
                startVpn()
                vpnStatus = "VPN Started"
            }, modifier = Modifier.padding(8.dp)) {
                Text("Start VPN")
            }

            Button(onClick = {
                stopVpn()
                vpnStatus = "VPN Stopped"
            }, modifier = Modifier.padding(8.dp)) {
                Text("Stop VPN")
            }
            if (profilePath != null){
                Text("Profile: $profilePath")
            }
            FilePickerScreen()

        }

    }

    private fun startVpn() {
        tunIntent = VpnService.prepare(this)
        if (tunIntent != null) {
            // 返回非空Intent，说明需要用户授权
            startActivityForResult(tunIntent!!, 0)
        } else {
            tunIntent = TunService::class.intent
            startService(tunIntent!!)
            Toast.makeText(this, "VPN Started", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopVpn() {
        tunService?.stopVpn()

        Toast.makeText(this, "VPN Stopped", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun FilePickerScreen() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("file_prefs", Context.MODE_PRIVATE)

    var savedFilePath by remember { mutableStateOf<String?>(sharedPreferences.getString("profile_path", null)) }

    val result = remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        result.value = it
    }

    Column {
        Button(onClick = {
            launcher.launch(arrayOf("*/*"))

        }) {
            Text(text = "Choose File")
        }
        result.value?.let { file ->
            Text(text = "Path: $file")
            Button(onClick = {
                val filePath = saveFileToAppDirectory(context, file)
                sharedPreferences.edit().putString("profile_path", filePath).apply()
                savedFilePath = filePath

            }) {
                Text(text = "Save File")
            }
        }

        savedFilePath?.let {
            Text(text = "Saved file path: $it")
        }
    }
}


fun saveFileToAppDirectory(context: Context, uri: Uri): String? {


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