package rs.clash.android.service

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
import uniffi.clash_android_ffi.SocketProtector
import java.io.File

var tunService: TunService? = null

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
        return START_STICKY
    }

    // Only invoked once
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
        // Route all network traffic
        builder.addRoute("0.0.0.0", 0)
        builder.addDnsServer("10.0.0.2")

        // BYPASS THIS APP'S TRAFFIC - prevents routing loops!
        // This ensures clash app's own traffic doesn't get routed through VPN tunnel
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
            object : SocketProtector {
                override fun protect(fd: Int) {
                    this@TunService.protect(fd)
                }
            },
        )
    }

    fun stopVpn() {
        vpnInterface?.close()
        vpnInterface = null
        stopSelf()
    }
}
