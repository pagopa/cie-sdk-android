package it.pagopa.io.app.cie_example.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pagopa.io.app.cie_example.R
import it.pagopa.io.app.cie_example.ui.AppNumberTextField
import it.pagopa.io.app.cie_example.ui.NfcDialog
import it.pagopa.io.app.cie_example.ui.PrimaryButton
import it.pagopa.io.app.cie_example.ui.model.LazyButtonModel
import it.pagopa.io.app.cie_example.ui.model.PaceReadDto
import it.pagopa.io.app.cie_example.ui.model.toPaceReadDto
import it.pagopa.io.app.cie_example.ui.view_model.PaceViewModel

@Composable
fun PaceProtocol(viewModel: PaceViewModel?, onNavigateToPaceRead: (PaceReadDto) -> Unit) {
    LaunchedEffect(key1 = viewModel) {
        viewModel?.resetMainUi()
    }
    NfcDialog(viewModel)
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(120.dp))
        AppNumberTextField(
            viewModel?.can,
            stringResource(R.string.insert_can),
        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            modifier = Modifier.padding(bottom = 16.dp), model = LazyButtonModel(
                textId = R.string.start
            ) {
                if (viewModel?.can?.value?.isNotEmpty() == true)
                    viewModel.showDialog.value = true
            }
        )
    }
    viewModel?.eMRTDResponse?.value?.let {
        viewModel.resetMainUi()
        onNavigateToPaceRead.invoke(it.toPaceReadDto())
    }
}