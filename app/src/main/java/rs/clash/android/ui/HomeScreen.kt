package rs.clash.android.ui

import android.app.Activity.RESULT_OK
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import rs.clash.android.Global
import rs.clash.android.viewmodel.HomeViewModel

@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(
    navigator: DestinationsNavigator,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    var vpnStatus by remember { mutableStateOf("VPN Stopped") }
    val profilePath by viewModel.profilePath.observeAsState()

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { it ->
            if (it.resultCode == RESULT_OK) {
                Toast.makeText(Global.application, "VPN Service Authorization success", Toast.LENGTH_SHORT).show()
            }
        }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = vpnStatus, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))

        Button(onClick = {
            viewModel.startVpn(launcher)
            vpnStatus = "VPN Started"
        }, modifier = Modifier.padding(8.dp)) {
            Text("Start VPN")
        }

        Button(onClick = {
            viewModel.stopVpn()
            vpnStatus = "VPN Stopped"
        }, modifier = Modifier.padding(8.dp)) {
            Text("Stop VPN")
        }
        if (profilePath != null) {
            Text("Profile: $profilePath")
        }
    }
}
