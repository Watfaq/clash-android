package rs.clash.android

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow

object Global : CoroutineScope by CoroutineScope(Dispatchers.IO) {
    val application: Application
        get() = application_

    private lateinit var application_: Application

    var profilePath: String = ""

    val isServiceRunning = MutableStateFlow(false)

    fun init(application: Application) {
        this.application_ = application
    }

    fun destroy() {
        cancel()
    }
}
