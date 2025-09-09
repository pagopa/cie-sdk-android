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
import it.pagopa.io.app.cie_example.ui.model.NisAndPaceReadDto
import it.pagopa.io.app.cie_example.ui.model.toNisAndPaceReadDto
import it.pagopa.io.app.cie_example.ui.view_model.NisAndPaceViewModel

@Composable
fun NisAndPaceView(
    viewModel: NisAndPaceViewModel?,
    onNavigateToNisAndPaceRead: (NisAndPaceReadDto) -> Unit
) {
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
        Spacer(Modifier.height(16.dp))
        AppNumberTextField(
            viewModel?.challenge,
            stringResource(R.string.insert_challenge),
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
    viewModel?.intAuthMRTDResponseRead?.value?.let {
        viewModel.resetMainUi()
        val nisAndPaceReadDto = (it.internalAuthentication to it.mrtd).toNisAndPaceReadDto()
        onNavigateToNisAndPaceRead.invoke(nisAndPaceReadDto)
    }
}