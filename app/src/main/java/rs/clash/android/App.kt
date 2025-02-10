package rs.clash.android

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.rememberNavHostEngine
import rs.clash.android.ui.BottomBar

@Composable
fun ClashApp(){
    val engine = rememberNavHostEngine()
    val navCtrl = engine.rememberNavController()

    Scaffold(
        bottomBar = { BottomBar(navCtrl) }
    ) { innerPadding ->
        DestinationsNavHost(
            engine = engine,
            navController = navCtrl,
            navGraph = NavGraphs.root,
            modifier = Modifier.padding(innerPadding),
        )
    }
}