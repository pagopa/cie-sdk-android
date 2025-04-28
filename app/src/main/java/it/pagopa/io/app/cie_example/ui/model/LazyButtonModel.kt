package it.pagopa.io.app.cie_example.ui.model

import androidx.annotation.StringRes
import java.io.Serializable

data class LazyButtonModel(
    @StringRes val textId: Int,
    val isVisible: Boolean = true,
    var ctrlOk: Boolean? = null,
    val onClick: () -> Unit
) : Serializable
