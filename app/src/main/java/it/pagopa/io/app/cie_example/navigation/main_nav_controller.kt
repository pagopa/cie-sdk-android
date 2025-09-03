package it.pagopa.io.app.cie_example.navigation

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
import it.pagopa.io.app.cie.CieSDK
import it.pagopa.io.app.cie_example.BuildConfig
import it.pagopa.io.app.cie_example.MainActivity
import it.pagopa.io.app.cie_example.R
import it.pagopa.io.app.cie_example.ui.header.BackArrowIcon
import it.pagopa.io.app.cie_example.ui.header.HeaderImage
import it.pagopa.io.app.cie_example.ui.header.HomeIcon
import it.pagopa.io.app.cie_example.ui.header.hideAll
import it.pagopa.io.app.cie_example.ui.view.CieSdkMethods
import it.pagopa.io.app.cie_example.ui.view.HomeView
import it.pagopa.io.app.cie_example.ui.view.PaceProtocol
import it.pagopa.io.app.cie_example.ui.view.ReadCie
import it.pagopa.io.app.cie_example.ui.view.ReadNis
import it.pagopa.io.app.cie_example.ui.view_model.CieSdkMethodsViewModel
import it.pagopa.io.app.cie_example.ui.view_model.PaceViewModel
import it.pagopa.io.app.cie_example.ui.view_model.ReadCieViewModel
import it.pagopa.io.app.cie_example.ui.view_model.ReadNisViewModel
import it.pagopa.io.app.cie_example.ui.view_model.dependenciesInjectedViewModel

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
            HomeView(onClick = {
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
            CieSdkMethods(vm, onNavigate = {
                navController.navigate(Routes.ReadCIE)
            }, onNavigateToNisAuth = {
                navController.navigate(Routes.ReadNIS)
            },onNavigateToPaceAuth = {
                navController.navigate(Routes.PaceAuth)
            })
        }
        composable<Routes.ReadCIE> {
            headerLeft.BackArrowIcon(navController)
            headerRight.HomeIcon(navController)
            titleResId.intValue = R.string.auth_with_cie
            val ctx = LocalContext.current
            val cieSdk = CieSDK.withContext(ctx).withCustomIdpUrl(BuildConfig.BASE_URL_IDP)
            val vm = dependenciesInjectedViewModel<ReadCieViewModel>(cieSdk)
            ReadCie(vm)
        }

        composable<Routes.ReadNIS> {
            headerLeft.BackArrowIcon(navController)
            headerRight.HomeIcon(navController)
            titleResId.intValue = R.string.reading_nis
            val cieSdk = CieSDK.withContext(LocalContext.current)
            val vm = dependenciesInjectedViewModel<ReadNisViewModel>(cieSdk)
            ReadNis(vm)
        }
        composable<Routes.PaceAuth> {
            headerLeft.BackArrowIcon(navController)
            headerRight.HomeIcon(navController)
            titleResId.intValue = R.string.reading_pace
            val cieSdk = CieSDK.withContext(LocalContext.current)
            val vm = dependenciesInjectedViewModel<PaceViewModel>(cieSdk)
            PaceProtocol(vm)
        }
    }
}
