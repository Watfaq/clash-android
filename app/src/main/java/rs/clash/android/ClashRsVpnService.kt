package rs.clash.android;

import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream

class ClashRsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunFd: Int? = null

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            runVpn()
        }
    }

    override fun onDestroy() {
        vpnInterface?.close()
        super.onDestroy()
    }

    private fun runVpn() {
        val builder = Builder()
        builder.setSession("ClashRS VPNService")
        builder.addAddress("10.0.0.2", 24)
        // Route all network traffic
        builder.addRoute("0.0.0.0", 0)
        vpnInterface = builder.establish()
        tunFd = vpnInterface?.detachFd()
        initClash(tunFd!!)

    }
}