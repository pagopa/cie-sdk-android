package it.pagopa.cie_sdk.ui.view

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import it.pagopa.cie.CieSDK
import it.pagopa.cie_sdk.R

@Composable
fun CieSdkMethods() {
    val ctx = LocalContext.current
    val cieSdk = CieSDK()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            Toast.makeText(ctx, cieSdk.hasNfc(ctx).toString(), Toast.LENGTH_LONG).show()
        }) {
            Text(stringResource(R.string.has_nfc))
        }
        Button(onClick = {
            Toast.makeText(ctx, cieSdk.isNfcAvailable(ctx).toString(), Toast.LENGTH_LONG).show()
        }) {
            Text(stringResource(R.string.has_nfc_enabled))
        }
    }
}