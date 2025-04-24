package it.pagopa.io.app.cie_example.navigation

import kotlinx.serialization.Serializable

sealed interface Routes {
    @Serializable
    data object Home : Routes

    @Serializable
    data object CieSdkMethods : Routes

    @Serializable
    data object ReadCIE : Routes
}