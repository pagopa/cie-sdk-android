package it.pagopa.cie_sdk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import it.pagopa.cie_sdk.navigation.CieSdkNavHost
import it.pagopa.cie_sdk.theme.CieSDKPocTheme
import it.pagopa.cie_sdk.ui.ThemePreviews
import it.pagopa.cie_sdk.ui.header.TopBar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            this.MainApp()
        }
    }
}

@Composable
fun MainActivity?.MainApp() {
    CieSDKPocTheme {
        val navController = rememberNavController()
        Scaffold(modifier = Modifier.fillMaxSize(),
            topBar = {
                TopBar(
                    titleResId = R.string.app_name
                )
            }
        ) { innerPadding ->
            this.CieSdkNavHost(navController, innerPadding)
        }
    }
}

@ThemePreviews
@Composable
fun MainAppPreview() {
    null.MainApp()
}