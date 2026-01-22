package rs.clash.android.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import rs.clash.android.MainActivity
import rs.clash.android.R

object NotificationHelper {
	private const val CHANNEL_ID = "clash_vpn_service"
	private const val CHANNEL_NAME = "Clash VPN Service"
	const val NOTIFICATION_ID = 1

	fun createNotificationChannel(context: Context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel =
				NotificationChannel(
					CHANNEL_ID,
					CHANNEL_NAME,
					NotificationManager.IMPORTANCE_LOW,
				).apply {
					description = "Clash VPN 服务运行通知"
					setShowBadge(false)
				}

			val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannel(channel)
		}
	}

	fun createNotification(context: Context): Notification {
		createNotificationChannel(context)

		val intent =
			Intent(context, MainActivity::class.java).apply {
				flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
			}

		val pendingIntent =
			PendingIntent.getActivity(
				context,
				0,
				intent,
				PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
			)

		return NotificationCompat
			.Builder(context, CHANNEL_ID)
			.setContentTitle("Clash VPN")
			.setContentText("VPN 服务正在运行")
			.setSmallIcon(R.drawable.ic_launcher_foreground)
			.setContentIntent(pendingIntent)
			.setOngoing(true)
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.setCategory(NotificationCompat.CATEGORY_SERVICE)
			.build()
	}
}
