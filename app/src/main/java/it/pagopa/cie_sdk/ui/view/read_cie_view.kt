package it.pagopa.cie_sdk.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pagopa.cie_sdk.R
import it.pagopa.cie_sdk.ui.PasswordTextField
import it.pagopa.cie_sdk.ui.PrimaryButton
import it.pagopa.cie_sdk.ui.ThemePreviews
import it.pagopa.cie_sdk.ui.model.LazyButtonModel
import it.pagopa.cie_sdk.ui.view_model.ReadCieViewModel

@Composable
fun ReadCie(viewModel: ReadCieViewModel?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(120.dp))
        PasswordTextField(
            viewModel?.pin,
            stringResource(R.string.insert_pin),
        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(modifier = Modifier.padding(bottom = 16.dp), model = LazyButtonModel(
            textId = R.string.start
        ) {

        })
    }
}

@ThemePreviews
@Composable
fun ReadCiePreview() {
    ReadCie(null)
}
