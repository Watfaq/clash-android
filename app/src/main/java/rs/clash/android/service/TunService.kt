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
import kotlinx.coroutines.Job
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
	companion object {
		const val ACTION_START_CORE = "rs.clash.android.action.START_CORE"
		const val ACTION_STOP_CORE = "rs.clash.android.action.STOP_CORE"
		private const val EXTRA_FORCE_FOREGROUND = "rs.clash.android.extra.FORCE_FOREGROUND"

		fun createStartIntent(
			context: Context,
			forceForeground: Boolean = false,
		): Intent =
			Intent(context, TunService::class.java)
				.setAction(ACTION_START_CORE)
				.putExtra(EXTRA_FORCE_FOREGROUND, forceForeground)

		fun createStopIntent(context: Context): Intent = Intent(context, TunService::class.java).setAction(ACTION_STOP_CORE)
	}

	private var vpnInterface: ParcelFileDescriptor? = null
	private var tunFd: Int? = null
	private var runVpnJob: Job? = null
	private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
	private var isDestroying = false

	override fun onStartCommand(
		intent: Intent?,
		flags: Int,
		startId: Int,
	): Int {
		val forceForeground = intent?.getBooleanExtra(EXTRA_FORCE_FOREGROUND, false) == true
		Log.i("clash", "onStartCommand forceForeground=$forceForeground")
		when (intent?.action) {
			ACTION_STOP_CORE -> {
				Log.i("clash", "Received stop action from shortcut/notification")
				stopVpn()
				return START_NOT_STICKY
			}
		}

		synchronized(this) {
			isDestroying = false
		}
		if (runVpnJob?.isActive == true) {
			Log.i("clash", "VPN job already running, skip duplicate start")
			return START_STICKY
		}

		startForegroundServiceIfNeeded(forceForeground)

		runVpnJob =
			serviceScope.launch {
				try {
					runVpn()
				} catch (e: Exception) {
					Log.e("clash", "Error in runVpn", e)
					stopVpn()
				}
			}

		tunService = this
		CoreToggleTileService.requestStateSync(this)
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
		val profilePath = resolveProfilePath()
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
		val established =
			builder.establish()
				?: throw IllegalStateException("Failed to establish VPN interface")
		vpnInterface = established
		tunFd = established.fd
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

		val finalProfile =
			runClash(
				profilePath,
				Global.application.cacheDir.toString(),
				ProfileOverride(
					established.fd,
					fakeIp = prefs.getBoolean("fake_ip", false),
					ipv6 = prefs.getBoolean("ipv6", true),
				),
			)
		Global.proxyPort = finalProfile.mixedPort
		Global.isServiceRunning.value = true
		CoreToggleTileService.requestStateSync(this)
	}

	private fun resolveProfilePath(): String {
		// App process may be recreated when started from Quick Settings tile.
		// Restore selected profile path from persistent storage before starting core.
		if (Global.profilePath.isBlank()) {
			val prefs = Global.application.getSharedPreferences("file_prefs", Context.MODE_PRIVATE)
			Global.profilePath = prefs.getString("profile_path", null).orEmpty()
		}

		val path = Global.profilePath.trim()
		if (path.isEmpty()) {
			throw IllegalStateException("No profile selected")
		}

		val configFile = File(path)
		if (!configFile.exists() || !configFile.isFile) {
			throw IllegalStateException("Profile file not found: $path")
		}

		return path
	}

	private fun startForegroundServiceIfNeeded(forceForeground: Boolean) {
		val prefs = Global.application.getSharedPreferences("settings", Context.MODE_PRIVATE)
		val foregroundServiceEnabled = prefs.getBoolean("foreground_service_enabled", false)
		val shouldStartForeground = forceForeground || foregroundServiceEnabled

		if (shouldStartForeground) {
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
		runVpnJob?.cancel()
		runVpnJob = null

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
		CoreToggleTileService.requestStateSync(this)
	}

	fun stopVpn() {
		cleanup()
		stopSelf()
	}
}
