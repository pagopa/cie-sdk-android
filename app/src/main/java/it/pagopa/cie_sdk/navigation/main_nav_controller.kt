package it.pagopa.cie_sdk.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import it.pagopa.cie.CieSDK
import it.pagopa.cie_sdk.MainActivity
import it.pagopa.cie_sdk.ui.UserInteraction
import it.pagopa.cie_sdk.ui.header.BackArrowIcon
import it.pagopa.cie_sdk.ui.header.HeaderImage
import it.pagopa.cie_sdk.ui.header.HomeIcon
import it.pagopa.cie_sdk.ui.header.hideAll
import it.pagopa.cie_sdk.ui.view.CieSdkMethods
import it.pagopa.cie_sdk.ui.view.HomeView
import it.pagopa.cie_sdk.R
import it.pagopa.cie_sdk.ui.view.ReadCie
import it.pagopa.cie_sdk.ui.view_model.CieSdkMethodsViewModel
import it.pagopa.cie_sdk.ui.view_model.ReadCieViewModel
import it.pagopa.cie_sdk.ui.view_model.dependenciesInjectedViewModel

@Composable
fun MainActivity?.CieSdkNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    headerLeft: MutableState<HeaderImage?>,
    titleResId: MutableIntState,
    headerRight: MutableState<HeaderImage?>
) {
    NavHost(
        navController = navController,
        modifier = Modifier.padding(innerPadding),
        startDestination = Routes.Home
    ) {
        composable<Routes.Home> {
            listOf(headerLeft, headerRight).hideAll()
            titleResId.intValue = R.string.app_name
            HomeView(onClick = UserInteraction {
                navController.navigate(Routes.CieSdkMethods)
            })
        }
        composable<Routes.CieSdkMethods> {
            headerLeft.BackArrowIcon(navController)
            headerRight.HomeIcon(navController)
            titleResId.intValue = R.string.test_methods_title
            val ctx = LocalContext.current
            val cieSdk = CieSDK.withContext(ctx)
            val vm = dependenciesInjectedViewModel<CieSdkMethodsViewModel>(cieSdk)
            CieSdkMethods(vm, onNavigate = UserInteraction {
                navController.navigate(Routes.ReadCIE)
            })
        }
        composable<Routes.ReadCIE> {
            headerLeft.BackArrowIcon(navController)
            headerRight.HomeIcon(navController)
            titleResId.intValue = R.string.auth_with_cie
            val ctx = LocalContext.current
            val cieSdk = CieSDK.withContext(ctx)
            val vm = dependenciesInjectedViewModel<ReadCieViewModel>(cieSdk)
            ReadCie(vm)
        }
    }
}
