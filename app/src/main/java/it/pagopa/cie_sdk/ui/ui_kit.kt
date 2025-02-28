package it.pagopa.cie_sdk.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import it.pagopa.cie_sdk.R
import it.pagopa.cie_sdk.theme.CieSDKPocTheme
import it.pagopa.cie_sdk.ui.model.LazyButtonModel
import it.pagopa.cie_sdk.ui.view_model.BaseViewModelWithNfcDialog

fun interface UserInteraction {
    fun action()
}

@Composable
fun BasePreview(content: @Composable () -> Unit) {
    CieSDKPocTheme {
        content()
    }
}

@Composable
fun PrimaryButton(modifier: Modifier = Modifier, model: LazyButtonModel) {
    Button(
        onClick = model.onClick,
        modifier = modifier
    ) {
        Text(stringResource(model.textId))
    }
}

@Composable
fun PasswordTextField(
    password: MutableState<String>?,
    label: String
) {
    var passwordVisible = rememberSaveable { mutableStateOf(false) }
    TextField(
        value = password?.value.orEmpty(),
        onValueChange = { password?.value = it },
        label = {
            Text(label)
        },
        singleLine = true,
        placeholder = { Text(label) },
        visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        trailingIcon = {
            val image = if (passwordVisible.value)
                Icons.Filled.Visibility
            else Icons.Filled.VisibilityOff
            val description = if (passwordVisible.value) "Hide password" else "Show password"
            IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                Icon(imageVector = image, description)
            }
        }
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebView(
    url: String,
    webViewClient: WebViewClient = WebViewClient()
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                this.webViewClient = webViewClient
                this.settings.javaScriptEnabled = true
            }
        },
        update = { webView ->
            webView.loadUrl(url)
        }
    )
}

@Composable
fun NfcDialog(viewModel: BaseViewModelWithNfcDialog?) {
    val cardShakeController = rememberShakeController()
    val shakeController = rememberShakeController()
    if (viewModel?.showDialog?.value == true) {
        shakeController.shake(
            ShakeConfig(
                iterations = 50,
                intensity = 500f,
                rotateX = -20f,
                translateY = 20f
            )
        )
        cardShakeController.shake(
            ShakeConfig(
                iterations = 50,
                intensity = 500f,
                rotateX = 20f,
                translateY = 20f
            )
        )
        viewModel.readCie()
    }
    AppDialog(
        viewModel?.showDialog,
        R.string.read_cie_dialog_title,
        R.string.read_cie_dialog_description,
        R.string.ok,
        btnAction = {
            viewModel?.onDialogDismiss()
            viewModel?.showDialog?.value = false
        },
        onDismiss = {
            viewModel?.onDialogDismiss()
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
            modifier = Modifier.padding(4.dp),
            text = if (viewModel?.dialogMessage?.value?.isEmpty() == true)
                "cie reading status"
            else
                viewModel?.dialogMessage?.value.orEmpty(),
            color = MaterialTheme.colorScheme.onBackground
        )
        val errorMessage = viewModel?.errorMessage?.value
        if (errorMessage?.isNotEmpty() == true) {
            Spacer(Modifier.height(16.dp))
            Text(
                modifier = Modifier.padding(4.dp),
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }
        val successMessage = viewModel?.successMessage?.value
        if (successMessage?.isNotEmpty() == true) {
            Spacer(Modifier.height(16.dp))
            Text(
                modifier = Modifier.padding(4.dp),
                text = successMessage,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}