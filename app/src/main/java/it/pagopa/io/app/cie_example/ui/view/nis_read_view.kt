package it.pagopa.io.app.cie_example.ui.view


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.pagopa.io.app.cie_example.ui.view_model.NisReadViewModel
import it.pagopa.io.app.cie_example.ui.view_model.PaceReadViewModel


@Composable
fun NisReadView(vm: NisReadViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(
                rememberScrollState()
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("NIS:", color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(16.dp))
        Text(vm.nisDto.nisAuth, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(16.dp))
    }
}