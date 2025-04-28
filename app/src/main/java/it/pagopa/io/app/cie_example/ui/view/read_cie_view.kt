package it.pagopa.io.app.cie_example.ui.view

import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pagopa.io.app.cie_example.R
import it.pagopa.io.app.cie_example.ui.NfcDialog
import it.pagopa.io.app.cie_example.ui.PasswordTextField
import it.pagopa.io.app.cie_example.ui.PrimaryButton
import it.pagopa.io.app.cie_example.ui.SimpleLoader
import it.pagopa.io.app.cie_example.ui.ThemePreviews
import it.pagopa.io.app.cie_example.ui.WebView
import it.pagopa.io.app.cie_example.ui.model.LazyButtonModel
import it.pagopa.io.app.cie_example.ui.view_model.ReadCieViewModel

@Composable
fun ReadCie(viewModel: ReadCieViewModel?) {
    MainUI(viewModel)
    NfcDialog(viewModel)
    val showDialog = remember { mutableStateOf(true) }
    if (viewModel?.shouldShowUI?.value != true) {
        WebView(
            url = viewModel?.webViewUrl?.value ?: "",
            webViewClient = viewModel?.WebViewClientWithRedirect() ?: WebViewClient()
        )
        if (viewModel?.webViewLoader?.value == true)
            SimpleLoader(showDialog)
    }
}

@Composable
private fun MainUI(viewModel: ReadCieViewModel?) {
    if (viewModel?.shouldShowUI?.value == true) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(120.dp))
            PasswordTextField(
                viewModel.pin,
                stringResource(R.string.insert_pin),
            )
            Spacer(modifier = Modifier.weight(1f))
            PrimaryButton(modifier = Modifier.padding(bottom = 16.dp), model = LazyButtonModel(
                textId = R.string.start
            ) {
                if (viewModel.pin.value.isNotEmpty())
                    viewModel.showDialog.value = true
            })
        }
    }
}

@ThemePreviews
@Composable
fun ReadCiePreview() {
    ReadCie(null)
}
