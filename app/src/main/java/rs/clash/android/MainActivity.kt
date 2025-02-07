package rs.clash.android

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rs.clash.android.theme.ClashAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClashAndroidTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ClashScreen()
                }
            }
        }
    }
    @Composable
    fun ClashScreen() {
        var vpnStatus by remember { mutableStateOf("VPN Stopped") }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = vpnStatus, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))

            Button(onClick = {
                CoroutineScope(Dispatchers.Main).launch {
                    startVpn()
                    vpnStatus = "VPN Started"
                }
            }, modifier = Modifier.padding(8.dp)) {
                Text("Start VPN")
            }

            Button(onClick = {
                CoroutineScope(Dispatchers.Main).launch {
                    stopVpn()
                    vpnStatus = "VPN Stopped"
                }
            }, modifier = Modifier.padding(8.dp)) {
                Text("Stop VPN")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val intent = Intent(this, ClashRsVpnService::class.java)
            startService(intent)
            Toast.makeText(this, "VPN Started", Toast.LENGTH_SHORT).show()
        }
    }
    private suspend fun startVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            ActivityCompat.startActivityForResult(this, intent, 0, null)
        } else {
            onActivityResult(0, RESULT_OK, null)
        }
    }

    private suspend fun stopVpn() {
        val intent = Intent(this, ClashRsVpnService::class.java)
        stopService(intent)
        Toast.makeText(this, "VPN Stopped", Toast.LENGTH_SHORT).show()
    }
}



