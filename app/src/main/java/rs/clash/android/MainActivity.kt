package rs.clash.android

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.os.StrictMode
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
import rs.clash.android.theme.ClashAndroidTheme

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



