package rs.clash.android.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import rs.clash.android.viewmodel.ProfileViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Destination<RootGraph>()
@Composable
fun ProfileScreen(navigator: DestinationsNavigator, vm: ProfileViewModel = viewModel()) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("file_prefs", Context.MODE_PRIVATE)

    var savedFilePath by remember { mutableStateOf<String?>(sharedPreferences.getString("profile_path", null)) }

    val result = remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        result.value = it
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            launcher.launch(arrayOf("*/*"))

        }) {
            Text(text = "Choose File")
        }
        result.value?.let { file ->
            Text(text = "Path: $file")
            Button(onClick = {
                val filePath = vm.saveFileToAppDirectory(context, file)
                sharedPreferences.edit().putString("profile_path", filePath).apply()
                savedFilePath = filePath

            }) {
                Text(text = "Save File")
            }
        }

        savedFilePath?.let {
            Text(text = "Saved file path: $it")
        }
    }
}

