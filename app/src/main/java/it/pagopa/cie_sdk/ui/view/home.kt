package it.pagopa.cie_sdk.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import it.pagopa.cie_sdk.R
import it.pagopa.cie_sdk.ui.BasePreview
import it.pagopa.cie_sdk.ui.ThemePreviews
import it.pagopa.cie_sdk.ui.UserInteraction

@Composable
fun HomeView(onClick: UserInteraction) {
    Box(
        Modifier
            .fillMaxSize()
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center).clickable{
                onClick.action()
            },
            text = stringResource(R.string.hello),
            color = MaterialTheme.colorScheme.primary
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