package rs.clash.android.viewmodel

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import rs.clash.android.Global
import rs.clash.android.intent
import rs.clash.android.service.TunService
import rs.clash.android.service.tunService

class HomeViewModel : ViewModel() {
    var profilePath = MutableLiveData<String?>(null)
    var tunIntent: Intent? = null

    init {
        val context = Global.application.applicationContext
        val sharedPreferences = context.getSharedPreferences("file_prefs", MODE_PRIVATE)
        profilePath.value = sharedPreferences.getString("profile_path", null)
        Global.profilePath = profilePath.value ?: ""
        sharedPreferences.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == "profile_path") {
                profilePath.value = sharedPreferences.getString("profile_path", null)
                Global.profilePath = profilePath.value ?: ""
            }
        }
    }

    fun startVpn(launcher: ManagedActivityResultLauncher<Intent, ActivityResult>) {
        val app = Global.application
        tunIntent = VpnService.prepare(app)
        if (tunIntent != null) {
            // 返回非空Intent，说明需要用户授权
            launcher.launch(tunIntent!!)
        } else {
            tunIntent = TunService::class.intent
            app.startService(tunIntent!!)
            Toast.makeText(app, "VPN Started", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopVpn() {
        val app = Global.application
        tunService?.stopVpn()

        Toast.makeText(app, "VPN Stopped", Toast.LENGTH_SHORT).show()
    }
}
