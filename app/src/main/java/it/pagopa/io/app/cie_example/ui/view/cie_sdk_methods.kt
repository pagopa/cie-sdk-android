package it.pagopa.io.app.cie_example.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import it.pagopa.io.app.cie_example.ui.NfcDialog
import it.pagopa.io.app.cie_example.ui.PrimaryButton
import it.pagopa.io.app.cie_example.ui.UserInteraction
import it.pagopa.io.app.cie_example.ui.view_model.CieSdkMethodsViewModel

@Composable
fun CieSdkMethods(
    viewModel: CieSdkMethodsViewModel,
    onNavigate: UserInteraction
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(viewModel.provideLazyButtons {
            onNavigate.action()
        }) {
            PrimaryButton(model = it)
            when (it.ctrlOk) {
                true -> Icon(Icons.Default.Done, "ok", tint = MaterialTheme.colorScheme.primary)
                false -> Icon(Icons.Default.Clear, "no", tint = MaterialTheme.colorScheme.error)
                else -> Unit
            }
        }
    }
    if (viewModel.showDialog.value)
        NfcDialog(viewModel)
}
