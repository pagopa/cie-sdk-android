package it.pagopa.io.app.cie_example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie_example.navigation.CieSdkNavHost
import it.pagopa.io.app.cie_example.theme.CieSDKPocTheme
import it.pagopa.io.app.cie_example.ui.ThemePreviews
import it.pagopa.io.app.cie_example.ui.header.HeaderImage
import it.pagopa.io.app.cie_example.ui.header.TopBar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CieLogger.enabled = BuildConfig.DEBUG
        enableEdgeToEdge()
        setContent {
            this.MainApp()
        }
    }
}

@Composable
fun MainActivity?.MainApp() {
    CieSDKPocTheme {
        val headerImageLeft = remember { mutableStateOf<HeaderImage?>(null) }
        val headerImageRight = remember { mutableStateOf<HeaderImage?>(null) }
        val titleResId = remember { mutableIntStateOf(R.string.app_name) }
        val navController = rememberNavController()
        Scaffold(modifier = Modifier.fillMaxSize(),
            topBar = {
                TopBar(
                    titleResId = titleResId.intValue,
                    imageLeft = headerImageLeft.value,
                    imageRight = headerImageRight.value
                )
            }
        ) { innerPadding ->
            this.CieSdkNavHost(
                navController,
                innerPadding,
                headerImageLeft,
                titleResId,
                headerImageRight
            )
        }
    }
}

@ThemePreviews
@Composable
fun MainAppPreview() {
    null.MainApp()
}