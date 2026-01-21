package rs.clash.android.service

import android.annotation.SuppressLint
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rs.clash.android.Global
import rs.clash.android.ffi.initClash
import uniffi.clash_android_ffi.ProfileOverride
import java.io.File
import java.net.Inet4Address

var tunService: TunService? = null

@SuppressLint("VpnServicePolicy")
class TunService : VpnService() {
	private var vpnInterface: ParcelFileDescriptor? = null
	private var tunFd: Int? = null

	override fun onStartCommand(
		intent: Intent?,
		flags: Int,
		startId: Int,
	): Int {
		Log.i("clash", "onStartCommand")
		CoroutineScope(Dispatchers.Default).launch {
			runVpn()
		}

		tunService = this
		Global.isServiceRunning.value = true
		return START_STICKY
	}

	override fun onCreate() {
		super.onCreate()
	}

	override fun onRevoke() {
		stopVpn()
		super.onRevoke()
	}

	override fun onDestroy() {
		stopVpn()
		super.onDestroy()
	}

	private suspend fun runVpn() {
		val builder = Builder()
		builder.setSession("ClashRS VPNService")
		builder.addAddress("10.0.0.1", 30)
		builder.addRoute("0.0.0.0", 0)

		// builder.addDnsServer("10.0.0.2")
		builder.addDisallowedApplication(packageName)

		vpnInterface = builder.establish()

		tunFd = vpnInterface?.fd
		val assets = Global.application.assets
		listOf("Country.mmdb", "geosite.dat").forEach { name ->
			assets
				.open("clash-res/$name")
				.use { it ->
					val file = File("${Global.application.cacheDir}/$name")
					file.deleteOnExit()
					file.createNewFile()
					it.copyTo(file.outputStream())
				}
		}

		initClash(
			Global.profilePath,
			Global.application.cacheDir.toString(),
			ProfileOverride(tunFd!!, "${Global.application.cacheDir}/clash-rs.log"),
		)
	}

	fun stopVpn() {
		vpnInterface?.close()
		vpnInterface = null
		tunService = null
		Global.isServiceRunning.value = false
		stopSelf()
	}
}
