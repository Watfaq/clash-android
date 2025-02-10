package rs.clash.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import rs.clash.android.theme.ClashAndroidTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Global.init(application)

        // enableEdgeToEdge()
        setContent {
            ClashAndroidTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ClashApp()
                }
            }
        }
    }
}

