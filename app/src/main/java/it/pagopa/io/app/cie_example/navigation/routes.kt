package it.pagopa.io.app.cie_example.navigation

import it.pagopa.io.app.cie_example.ui.model.NisAndPaceReadDto
import it.pagopa.io.app.cie_example.ui.model.NisDto
import it.pagopa.io.app.cie_example.ui.model.PaceReadDto
import kotlinx.serialization.Serializable

sealed interface Routes {
    @Serializable
    data object Home : Routes

    @Serializable
    data object CieSdkMethods : Routes

    @Serializable
    data object ReadCIE : Routes

    @Serializable
    data object ReadCIECertificate : Routes

    @Serializable
    data object ReadNIS : Routes

    @Serializable
    data object PaceAuth : Routes

    @Serializable
    data object NisAndPaceAuth : Routes

    @Serializable
    data class NisRead(val nisDto: NisDto) : Routes

    @Serializable
    data class PaceRead(val paceReadDto: PaceReadDto) : Routes

    @Serializable
    data class NisAndPaceRead(val nisAndPaceReadDto: NisAndPaceReadDto) : Routes
}