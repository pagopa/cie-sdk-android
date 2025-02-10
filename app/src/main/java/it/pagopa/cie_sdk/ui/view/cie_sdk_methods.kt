package it.pagopa.cie_sdk.ui.view

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import it.pagopa.cie.CieSDK

@Composable
fun CieSdkMethods() {
    val ctx = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        Button(modifier = Modifier.align(Alignment.Center), onClick = {
            Toast.makeText(ctx, CieSDK().methodFromSdk(), Toast.LENGTH_LONG).show()
        }) {
            Text("Click me to use method from sdk")
        }
    }
}