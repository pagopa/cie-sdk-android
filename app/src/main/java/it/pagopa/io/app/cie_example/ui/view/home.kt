package it.pagopa.io.app.cie_example.ui.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import it.pagopa.io.app.cie_example.R
import it.pagopa.io.app.cie_example.ui.BasePreview
import it.pagopa.io.app.cie_example.ui.PrimaryButton
import it.pagopa.io.app.cie_example.ui.ThemePreviews
import it.pagopa.io.app.cie_example.ui.UserInteraction
import it.pagopa.io.app.cie_example.ui.model.LazyButtonModel

@Composable
fun HomeView(onClick: UserInteraction) {
    Box(
        Modifier
            .fillMaxSize()
    ) {
        PrimaryButton(modifier = Modifier.align(Alignment.Center),
            model = LazyButtonModel(R.string.hello) {
                onClick.action()
            }
        )
    }
}

@ThemePreviews
@Composable
fun PreviewHomeView() {
    BasePreview {
        HomeView {}
    }
}