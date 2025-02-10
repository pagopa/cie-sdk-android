package it.pagopa.cie_sdk.ui.header

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import it.pagopa.cie_sdk.R
import it.pagopa.cie_sdk.navigation.Routes
import it.pagopa.cie_sdk.ui.UserInteraction

data class HeaderImage(
    val icon: ImageVector,
    val contentDescription: String,
    val onIconClick: UserInteraction
)

fun MutableState<HeaderImage?>.hide() {
    this.value = null
}

fun List<MutableState<HeaderImage?>>.hideAll() {
    this.map { it.hide() }
}


@Composable
fun MutableState<HeaderImage?>.BackArrowIcon(navController: NavController) {
    this.value = HeaderImage(Icons.AutoMirrored.Default.ArrowBack,
        stringResource(R.string.back),
        onIconClick = UserInteraction {
            navController.popBackStack()
        })
}

@Composable
fun MutableState<HeaderImage?>.HomeIcon(navController: NavController) {
    this.value = HeaderImage(Icons.Default.Home,
        stringResource(R.string.home),
        onIconClick = UserInteraction {
            navController.popBackStack(Routes.Home, false)
        })
}
