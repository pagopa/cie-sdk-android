package it.pagopa.cie_sdk.navigation

import kotlinx.serialization.Serializable

sealed class Routes {
    @Serializable
    data object Home : Routes()

    @Serializable
    data object CieSdkMethods : Routes()
}