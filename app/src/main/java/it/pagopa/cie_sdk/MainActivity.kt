package it.pagopa.cie_sdk

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
import it.pagopa.cie.CieLogger
import it.pagopa.cie_sdk.navigation.CieSdkNavHost
import it.pagopa.cie_sdk.theme.CieSDKPocTheme
import it.pagopa.cie_sdk.ui.ThemePreviews
import it.pagopa.cie_sdk.ui.header.HeaderImage
import it.pagopa.cie_sdk.ui.header.TopBar

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