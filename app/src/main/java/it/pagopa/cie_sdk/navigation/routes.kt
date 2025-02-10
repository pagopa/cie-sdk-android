package it.pagopa.cie_sdk.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Home

sealed class Routes{
    @Serializable
    data object CieSdkMethods : Routes()
}