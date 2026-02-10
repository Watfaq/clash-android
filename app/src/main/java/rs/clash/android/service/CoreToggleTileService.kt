package rs.clash.android.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import rs.clash.android.Global
import rs.clash.android.MainActivity
import rs.clash.android.R

@RequiresApi(Build.VERSION_CODES.N)
class CoreToggleTileService : TileService() {
	override fun onTileAdded() {
		super.onTileAdded()
		updateTileState()
	}

	override fun onStartListening() {
		super.onStartListening()
		updateTileState()
	}

	override fun onClick() {
		super.onClick()
		if (isCoreRunning()) {
			renderTile(active = false)
			startService(TunService.createStopIntent(this))
			requestStateSync(this)
			return
		}

		val prepareIntent = VpnService.prepare(this)
		if (prepareIntent == null) {
			renderTile(active = true)
			val started =
				runCatching {
					startCoreService()
				}.onFailure { error ->
					Log.e("clash", "Failed to start core from tile", error)
				}.isSuccess
			if (started) {
				requestStateSync(this)
			} else {
				renderTile(active = false)
				Toast.makeText(this, R.string.tile_core_start_failed, Toast.LENGTH_SHORT).show()
			}
			return
		}

		val launchIntent =
			Intent(this, MainActivity::class.java).apply {
				flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
				putExtra(MainActivity.EXTRA_START_CORE_FROM_SHORTCUT, true)
			}
		launchFromTile(launchIntent)
		Toast.makeText(this, R.string.tile_vpn_permission_required, Toast.LENGTH_SHORT).show()
	}

	private fun isCoreRunning(): Boolean = tunService != null || Global.isServiceRunning.value

	private fun startCoreService() {
		val intent = TunService.createStartIntent(this, forceForeground = true)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startForegroundService(intent)
		} else {
			startService(intent)
		}
	}

	private fun updateTileState() {
		renderTile(active = isCoreRunning())
	}

	private fun renderTile(active: Boolean) {
		val tile = qsTile ?: return
		tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
		tile.label = getString(R.string.tile_core_toggle_label)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			tile.subtitle =
				getString(
					if (active) {
						R.string.tile_core_running
					} else {
						R.string.tile_core_stopped
					},
				)
		}
		tile.updateTile()
	}

	private fun launchFromTile(intent: Intent) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			val pendingIntent =
				PendingIntent.getActivity(
					this,
					0,
					intent,
					PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
				)
			startActivityAndCollapse(pendingIntent)
		} else {
			@Suppress("DEPRECATION")
			startActivityAndCollapse(intent)
		}
	}

	companion object {
		fun requestStateSync(context: Context) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				requestListeningState(context, ComponentName(context, CoreToggleTileService::class.java))
			}
		}
	}
}
