package rs.clash.android.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PanelScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ProfileScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import rs.clash.android.R

enum class BottomBarItem(
	val direction: DirectionDestinationSpec,
	val icon: ImageVector,
	@StringRes val label: Int,
) {
	Home(HomeScreenDestination, Icons.Outlined.Home, R.string.home_screen),
	Panel(PanelScreenDestination, Icons.Outlined.Dashboard, R.string.panel_screen),
	Profile(ProfileScreenDestination, Icons.AutoMirrored.Outlined.TextSnippet, R.string.profile_screen),
	Settings(SettingsScreenDestination, Icons.Outlined.Settings, R.string.settings_screen),
}

@Composable
fun BottomBar(
	navController: NavHostController,
	modifier: Modifier = Modifier,
) {
	val navigator = navController.rememberDestinationsNavigator()
	NavigationBar {
		BottomBarItem.entries.forEach { destination ->
			val isCurrentDestOnBackStack by navController.isRouteOnBackStackAsState(destination.direction)
			NavigationBarItem(
				selected = isCurrentDestOnBackStack,
				onClick = {
					if (isCurrentDestOnBackStack) {
						// When we click again on a bottom bar item and it was already selected
						// we want to pop the back stack until the initial destination of this bottom bar item
						navigator.popBackStack(destination.direction, false)
						return@NavigationBarItem
					}

					navigator.navigate(destination.direction) {
						// Pop up to the root of the graph to
						// avoid building up a large stack of destinations
						// on the back stack as users select items
						popUpTo(NavGraphs.root) {
							saveState = true
						}

						// Avoid multiple copies of the same destination when
						// re-selecting the same item
						launchSingleTop = true
						// Restore state when re-selecting a previously selected item
						restoreState = true
					}
				},
				icon = {
					Icon(
						destination.icon,
						contentDescription = stringResource(destination.label),
					)
				},
				label = { Text(stringResource(destination.label)) },
			)
		}
	}
}
