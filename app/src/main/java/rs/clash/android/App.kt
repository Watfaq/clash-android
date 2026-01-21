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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ProfileScreenDestination
import com.ramcosta.composedestinations.rememberNavHostEngine
import rs.clash.android.ui.BottomBar
import rs.clash.android.ui.SlideHorizontalTransitions
import rs.clash.android.viewmodel.HomeViewModel

@Composable
fun ClashApp(
	modifier: Modifier = Modifier,
	homeViewModel: HomeViewModel = viewModel(),
) {
	val engine = rememberNavHostEngine()
	val navCtrl = engine.rememberNavController()

	val vpnPermissionLauncher =
		rememberLauncherForActivityResult(
			contract = ActivityResultContracts.StartActivityForResult(),
			onResult = { result ->
				if (result.resultCode == RESULT_OK) {
					homeViewModel.startVpn()
				}
			},
		)

	val isVpnRunning = homeViewModel.isVpnRunning

	Scaffold(
		modifier = modifier,
		bottomBar = { BottomBar(navCtrl) },
		floatingActionButton = {
			VpnToggleFab(
				isVpnRunning = isVpnRunning,
				onToggleVpn =
					remember(isVpnRunning) {
						{
							if (isVpnRunning) {
								homeViewModel.stopVpn()
							} else {
								homeViewModel.startVpn(vpnPermissionLauncher)
							}
						}
					},
			)
		},
	) { innerPadding ->
		DestinationsNavHost(
			engine = engine,
			navController = navCtrl,
			navGraph = NavGraphs.root,
			modifier = Modifier.padding(innerPadding),
		) {
			HomeScreenDestination animateWith SlideHorizontalTransitions
			ProfileScreenDestination animateWith SlideHorizontalTransitions
		}
	}
}

@Composable
private fun VpnToggleFab(
	isVpnRunning: Boolean,
	onToggleVpn: () -> Unit,
	modifier: Modifier = Modifier,
) {
	FloatingActionButton(
		onClick = onToggleVpn,
		modifier = modifier,
	) {
		val (icon, contentDescription) =
			if (isVpnRunning) {
				Icons.Filled.Stop to "Stop VPN"
			} else {
				Icons.Filled.PlayArrow to "Start VPN"
			}
		Icon(
			imageVector = icon,
			contentDescription = contentDescription,
		)
	}
}
