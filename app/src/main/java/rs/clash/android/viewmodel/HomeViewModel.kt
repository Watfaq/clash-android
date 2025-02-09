package rs.clash.android.viewmodel

import android.content.Context.MODE_PRIVATE
import android.database.Observable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import rs.clash.android.Global

class HomeViewModel: ViewModel() {
    var profilePath = MutableLiveData<String?>(null)
    init {
        val context = Global.application.applicationContext
        val sharedPreferences = context.getSharedPreferences("file_prefs", MODE_PRIVATE)
        profilePath.value = sharedPreferences.getString("profile_path", null)

        sharedPreferences.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == "profile_path"){
                profilePath.value = sharedPreferences.getString("profile_path", null)
            }
        }

    }
}

