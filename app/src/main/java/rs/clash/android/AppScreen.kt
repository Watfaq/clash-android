package rs.clash.android

import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ConnectionsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PanelScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ProfileScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.rememberNavHostEngine
import rs.clash.android.theme.ScaleTransitions
import rs.clash.android.theme.SlideHorizontalTransitions
import rs.clash.android.ui.components.BottomBar
import rs.clash.android.ui.snackbar.SnackbarControllerProvider
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

	SnackbarControllerProvider { snackbarHostState ->
		Scaffold(
			modifier = modifier,
			bottomBar = { BottomBar(navCtrl) },
			snackbarHost = {
				SnackbarHost(hostState = snackbarHostState) { data ->
					Snackbar(snackbarData = data)
				}
			},
		) { innerPadding ->
			DestinationsNavHost(
				engine = engine,
				navController = navCtrl,
				navGraph = NavGraphs.root,
				modifier = Modifier.padding(innerPadding),
			) {
				HomeScreenDestination animateWith SlideHorizontalTransitions
				PanelScreenDestination animateWith SlideHorizontalTransitions
				ProfileScreenDestination animateWith SlideHorizontalTransitions
				SettingsScreenDestination animateWith SlideHorizontalTransitions
				ConnectionsScreenDestination animateWith ScaleTransitions
			}
		}
	}
}
