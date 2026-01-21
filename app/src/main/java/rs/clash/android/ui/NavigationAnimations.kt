package rs.clash.android.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.spec.DestinationStyle

object SlideHorizontalTransitions : DestinationStyle.Animated() {
	private fun getDestinationIndex(route: String?): Int {
		return BottomBarItem.entries.indexOfFirst { it.direction.route == route }
	}

	override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
		val targetIndex = getDestinationIndex(targetState.destination.route)
		val initialIndex = getDestinationIndex(initialState.destination.route)
		
		val direction = if (targetIndex > initialIndex) {
			AnimatedContentTransitionScope.SlideDirection.Left
		} else {
			AnimatedContentTransitionScope.SlideDirection.Right
		}
		
		slideIntoContainer(
			towards = direction,
			animationSpec = tween(300),
		)
	}

	override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
		val targetIndex = getDestinationIndex(targetState.destination.route)
		val initialIndex = getDestinationIndex(initialState.destination.route)
		
		val direction = if (targetIndex > initialIndex) {
			AnimatedContentTransitionScope.SlideDirection.Left
		} else {
			AnimatedContentTransitionScope.SlideDirection.Right
		}
		
		slideOutOfContainer(
			towards = direction,
			animationSpec = tween(300),
		)
	}

	override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
		slideIntoContainer(
			towards = AnimatedContentTransitionScope.SlideDirection.Right,
			animationSpec = tween(300),
		)
	}

	override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
		slideOutOfContainer(
			towards = AnimatedContentTransitionScope.SlideDirection.Right,
			animationSpec = tween(300),
		)
	}
}
