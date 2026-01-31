package rs.clash.android.ui.snackbar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

val LocalSnackbarController = staticCompositionLocalOf {
    SnackbarController(
        host = SnackbarHostState(),
        scope = CoroutineScope(EmptyCoroutineContext)
    )
}

class SnackbarController(
    private val host: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    fun showMessage(
        message: String,
        action: SnackbarAction? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
    ) {
        scope.launch {
            // Uncomment if you want snackbar to be displayed immediately
            // host.currentSnackbarData?.dismiss()
            val result = host.showSnackbar(
                message = message,
                actionLabel = action?.title,
                duration = duration
            )

            if (result == SnackbarResult.ActionPerformed) {
                action?.onActionPress?.invoke()
            }
        }
    }

    companion object {
        val current
            @Composable
            @ReadOnlyComposable
            get() = LocalSnackbarController.current

        private val channel = Channel<SnackbarChannelMessage>(capacity = Int.MAX_VALUE)

        fun showMessage(
            message: String,
            action: SnackbarAction? = null,
            duration: SnackbarDuration = SnackbarDuration.Short,
        ) {
            channel.trySend(
                SnackbarChannelMessage(
                    message = message,
                    duration = duration,
                    action = action
                )
            )
        }

        internal fun getChannel(): Channel<SnackbarChannelMessage> = channel
    }
}
