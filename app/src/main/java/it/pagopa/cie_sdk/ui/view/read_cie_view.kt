package it.pagopa.cie_sdk.ui.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pagopa.cie_sdk.R
import it.pagopa.cie_sdk.ui.AppDialog
import it.pagopa.cie_sdk.ui.PasswordTextField
import it.pagopa.cie_sdk.ui.PrimaryButton
import it.pagopa.cie_sdk.ui.ShakeConfig
import it.pagopa.cie_sdk.ui.ThemePreviews
import it.pagopa.cie_sdk.ui.model.LazyButtonModel
import it.pagopa.cie_sdk.ui.rememberShakeController
import it.pagopa.cie_sdk.ui.shake
import it.pagopa.cie_sdk.ui.view_model.ReadCieViewModel

@Composable
fun ReadCie(viewModel: ReadCieViewModel?) {
    val cardShakeController = rememberShakeController()
    val shakeController = rememberShakeController()
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(120.dp))
        PasswordTextField(
            viewModel?.pin,
            stringResource(R.string.insert_pin),
        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(modifier = Modifier.padding(bottom = 16.dp), model = LazyButtonModel(
            textId = R.string.start
        ) {
            if (viewModel == null) return@LazyButtonModel
            if (viewModel.pin.value.isNotEmpty())
                viewModel.showDialog.value = true
        })
    }
    if (viewModel?.showDialog?.value == true) {
        shakeController.shake(
            ShakeConfig(
                iterations = 20,
                intensity = 500f,
                rotateX = -20f,
                translateY = 20f
            )
        )
        cardShakeController.shake(
            ShakeConfig(
                iterations = 20,
                intensity = 500f,
                rotateX = 20f,
                translateY = 20f
            )
        )
        viewModel.readCie {
            viewModel.dialogMessage.value = it
        }
    }
    AppDialog(
        viewModel?.showDialog,
        R.string.read_cie_dialog_title,
        R.string.read_cie_dialog_description,
        R.string.ok,
        btnAction = {
            viewModel?.dialogMessage?.value = ""
        }
    ) {
        Box(
            Modifier
                .width(80.dp)
                .wrapContentHeight()
        ) {
            Image(
                painter = painterResource(R.drawable.card),
                contentDescription = null,
                modifier = Modifier
                    .width(64.dp)
                    .height(64.dp)
                    .shake(cardShakeController)
                    .align(Alignment.TopStart)
            )
            Image(
                painter = painterResource(R.drawable.phone),
                contentDescription = null,
                modifier = Modifier
                    .width(64.dp)
                    .height(64.dp)
                    .shake(shakeController)
                    .align(Alignment.TopEnd)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (viewModel?.dialogMessage?.value?.isEmpty() == true)
                "cie reading status"
            else
                viewModel?.dialogMessage?.value.orEmpty()
        )
    }
}

@ThemePreviews
@Composable
fun ReadCiePreview() {
    ReadCie(null)
}
