package rs.clash.android

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow

object Global : CoroutineScope by CoroutineScope(Dispatchers.IO) {
	val application: Application
		get() = applicationInstance

	private lateinit var applicationInstance: Application

	var profilePath: String = ""

	val isServiceRunning = MutableStateFlow(false)

	fun init(application: Application) {
		this.applicationInstance = application
	}

	fun destroy() {
		cancel()
	}
}
