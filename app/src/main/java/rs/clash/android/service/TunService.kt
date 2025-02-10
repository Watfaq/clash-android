package rs.clash.android.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rs.clash.android.ffi.initClash


var tunService: TunService? = null

class TunService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunFd: Int? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.i("clash","onStartCommand")
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
        builder.addAddress("10.0.0.2", 24)
        // Route all network traffic
        builder.addRoute("0.0.0.0", 0)

        vpnInterface = builder.establish()

        tunFd = vpnInterface?.fd
        initClash(tunFd!!)

    }

    fun stopVpn() {
        vpnInterface?.close()
        vpnInterface = null
    }
}