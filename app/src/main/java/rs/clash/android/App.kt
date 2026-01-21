package rs.clash.android

import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.rememberNavHostEngine
import rs.clash.android.ui.BottomBar
import rs.clash.android.viewmodel.HomeViewModel

@Composable
fun ClashApp(
	modifier: Modifier = Modifier,
	homeViewModel: HomeViewModel = viewModel(),
) {
	val engine = rememberNavHostEngine()
	val navCtrl = engine.rememberNavController()

	val launcher =
		rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
			if (it.resultCode == RESULT_OK) {
				// Permission granted, trigger start again
				homeViewModel.startVpn()
			}
		}

	Scaffold(
		bottomBar = { BottomBar(navCtrl) },
		floatingActionButton = {
			FloatingActionButton(onClick = {
				if (homeViewModel.isVpnRunning) {
					homeViewModel.stopVpn()
				} else {
					homeViewModel.startVpn(launcher)
				}
			}) {
				if (homeViewModel.isVpnRunning) {
					Icon(Icons.Filled.Stop, contentDescription = "Stop VPN")
				} else {
					Icon(Icons.Filled.PlayArrow, contentDescription = "Start VPN")
				}
			}
		},
	) { innerPadding ->
		DestinationsNavHost(
			engine = engine,
			navController = navCtrl,
			navGraph = NavGraphs.root,
			modifier = Modifier.padding(innerPadding),
		)
	}
}
