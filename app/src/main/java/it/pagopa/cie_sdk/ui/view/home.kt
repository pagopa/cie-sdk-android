package it.pagopa.cie_sdk.ui.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import it.pagopa.cie_sdk.R
import it.pagopa.cie_sdk.ui.BasePreview
import it.pagopa.cie_sdk.ui.PrimaryButton
import it.pagopa.cie_sdk.ui.ThemePreviews
import it.pagopa.cie_sdk.ui.UserInteraction
import it.pagopa.cie_sdk.ui.model.LazyButtonModel

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