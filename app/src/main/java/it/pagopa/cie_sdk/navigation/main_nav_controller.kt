package it.pagopa.cie_sdk.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import it.pagopa.cie_sdk.MainActivity
import it.pagopa.cie_sdk.ui.UserInteraction
import it.pagopa.cie_sdk.ui.view.CieSdkMethods
import it.pagopa.cie_sdk.ui.view.HomeView

@Composable
fun MainActivity?.CieSdkNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
) {
    NavHost(
        navController = navController,
        modifier = Modifier.padding(innerPadding),
        startDestination = Routes.Home
    ) {
        composable<Routes.Home> {
            HomeView(onClick = UserInteraction {
                navController.navigate(Routes.CieSdkMethods)
            })
        }
        composable<Routes.CieSdkMethods> {
            CieSdkMethods()
        }
    }
}
