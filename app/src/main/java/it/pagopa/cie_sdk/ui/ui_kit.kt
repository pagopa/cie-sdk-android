package it.pagopa.cie_sdk.ui

import androidx.compose.runtime.Composable
import it.pagopa.cie_sdk.theme.CieSDKPocTheme

fun interface UserInteraction{
    fun action()
}

@Composable
fun BasePreview(content: @Composable () -> Unit) {
    CieSDKPocTheme {
        content()
    }
}