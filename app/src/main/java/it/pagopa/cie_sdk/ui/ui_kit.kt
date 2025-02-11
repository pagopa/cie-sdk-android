package it.pagopa.cie_sdk.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import it.pagopa.cie_sdk.theme.CieSDKPocTheme
import it.pagopa.cie_sdk.ui.model.LazyButtonModel

fun interface UserInteraction {
    fun action()
}

@Composable
fun BasePreview(content: @Composable () -> Unit) {
    CieSDKPocTheme {
        content()
    }
}

@Composable
fun PrimaryButton(modifier: Modifier, model: LazyButtonModel) {
    Button(
        onClick = model.onClick,
        modifier = modifier
    ) {
        Text(stringResource(model.textId))
    }
}