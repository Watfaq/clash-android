package rs.clash.android.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import rs.clash.android.Global
import rs.clash.android.util.NotificationHelper
import rs.clash.android.util.PermissionHelper
import uniffi.clash_android_ffi.ProfileOverride
import uniffi.clash_android_ffi.runClash
import uniffi.clash_android_ffi.shutdown
import java.io.File

var tunService: TunService? = null

@SuppressLint("VpnServicePolicy")
class TunService : VpnService() {
	private var vpnInterface: ParcelFileDescriptor? = null
	private var tunFd: Int? = null
	private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
	private var isDestroying = false

	override fun onStartCommand(
		intent: Intent?,
		flags: Int,
		startId: Int,
	): Int {
		Log.i("clash", "onStartCommand")

		startForegroundServiceIfNeeded()

		serviceScope.launch {
			try {
				runVpn()
			} catch (e: Exception) {
				Log.e("clash", "Error in runVpn", e)
				stopVpn()
			}
		}

		tunService = this
		Global.isServiceRunning.value = true
		return START_STICKY
	}

	override fun onCreate() {
		super.onCreate()
	}

	override fun onRevoke() {
		Log.i("clash", "onRevoke called")
		cleanup()
		super.onRevoke()
	}

	override fun onDestroy() {
		Log.i("clash", "onDestroy called")
		cleanup()
		super.onDestroy()
	}

	private suspend fun runVpn() {
		val prefs = Global.application.getSharedPreferences("settings", Context.MODE_PRIVATE)
		val builder = Builder()
		builder.setSession("ClashRS VPNService")
		builder.addAddress("10.0.0.1", 30)
		builder.addRoute("0.0.0.0", 0)

		builder.addDnsServer("10.0.0.2")
		
		// Apply app filter settings
		val appFilterMode = prefs.getString("app_filter_mode", "ALL") ?: "ALL"
		when (appFilterMode) {
			"ALLOWED" -> {
				val allowedApps = prefs.getStringSet("allowed_apps", emptySet()) ?: emptySet()
				allowedApps.forEach { packageName ->
					try {
						builder.addAllowedApplication(packageName)
						Log.d("clash", "Added allowed app: $packageName")
					} catch (e: Exception) {
						Log.e("clash", "Failed to add allowed app: $packageName", e)
					}
				}
			}
			"DISALLOWED" -> {
				val disallowedApps = prefs.getStringSet("disallowed_apps", emptySet()) ?: emptySet()
				// Always disallow self
				builder.addDisallowedApplication(packageName)
				disallowedApps.forEach { packageName ->
					try {
						builder.addDisallowedApplication(packageName)
						Log.d("clash", "Added disallowed app: $packageName")
					} catch (e: Exception) {
						Log.e("clash", "Failed to add disallowed app: $packageName", e)
					}
				}
			}
			else -> {
				// ALL mode - disallow only self
				builder.addDisallowedApplication(packageName)
			}
		}
		
		builder.allowBypass()
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

		val finalProfile = runClash(
			Global.profilePath,
			Global.application.cacheDir.toString(),
			ProfileOverride(
				tunFd!!,
				fakeIp = prefs.getBoolean("fake_ip", false),
				ipv6 = prefs.getBoolean("ipv6", true),
			),
		)
		Global.proxyPort = finalProfile.mixedPort
	}

	private fun startForegroundServiceIfNeeded() {
		val prefs = Global.application.getSharedPreferences("settings", Context.MODE_PRIVATE)
		val foregroundServiceEnabled = prefs.getBoolean("foreground_service_enabled", false)

		if (foregroundServiceEnabled) {
			// 检查通知权限
			if (!PermissionHelper.hasNotificationPermission(this)) {
				Log.w("clash", "Foreground service is enabled but notification permission is not granted")
				// 即使没有权限，仍然尝试启动前台服务（Android 13以下不需要权限）
			}

			val notification = NotificationHelper.createNotification(this)

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
				startForeground(
					NotificationHelper.NOTIFICATION_ID,
					notification,
					ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
				)
			} else {
				startForeground(NotificationHelper.NOTIFICATION_ID, notification)
			}

			Log.i("clash", "Started foreground service with SPECIAL_USE type")
		} else {
			Log.i("clash", "Foreground service is disabled")
		}
	}

	private fun cleanup() {
		synchronized(this) {
			if (isDestroying) {
				return
			}
			isDestroying = true
		}
		Log.i("clash", "Cleaning up VPN service")
		// pass `shutdown` t0 clash-lib
		shutdown()

		try {
			vpnInterface?.close()
		} catch (e: Exception) {
			Log.e("clash", "Error closing VPN interface", e)
		}

		vpnInterface = null
		tunFd = null
		tunService = null
		Global.proxyPort = null
		Global.isServiceRunning.value = false
		isDestroying = false
	}

	fun stopVpn() {
		cleanup()
		stopSelf()
	}
}
