package it.pagopa.io.app.cie_example.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults.drawStopIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import it.pagopa.io.app.cie.CieSDK
import it.pagopa.io.app.cie_example.R
import it.pagopa.io.app.cie_example.theme.CieSDKPocTheme
import it.pagopa.io.app.cie_example.ui.model.LazyButtonModel
import it.pagopa.io.app.cie_example.ui.view_model.BaseViewModelWithNfcDialog
import it.pagopa.io.app.cie_example.ui.view_model.CieSdkMethodsViewModel
import java.util.Locale

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
fun NfcDialog(
    viewModel: BaseViewModelWithNfcDialog?
) {
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
            modifier = Modifier.padding(horizontal = 4.dp),
            text = if (viewModel?.dialogMessage?.value?.isEmpty() == true)
                ""
            else
                viewModel?.dialogMessage?.value.orEmpty(),
            color = MaterialTheme.colorScheme.onBackground
        )
        val errorMessage = viewModel?.errorMessage?.value
        if (errorMessage?.isNotEmpty() == true) {
            Spacer(Modifier.height(16.dp))
            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }
        val successMessage = viewModel?.successMessage?.value
        if (successMessage?.isNotEmpty() == true) {
            Spacer(Modifier.height(16.dp))
            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = successMessage,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (viewModel?.showProgress?.value == true) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .wrapContentHeight()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val base = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                val strokeCap = StrokeCap.Round
                LinearProgressIndicator(
                    modifier = Modifier
                        .wrapContentHeight()
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = base,
                    strokeCap = strokeCap,
                    gapSize = 0.dp,
                    progress = {
                        viewModel.progressValue.floatValue
                    },
                    drawStopIndicator = {
                        drawStopIndicator(
                            drawScope = this,
                            stopSize = 0.dp,
                            color = base,
                            strokeCap = strokeCap
                        )
                    }
                )
                Text(
                    color = MaterialTheme.colorScheme.primary,
                    text = String.format(
                        Locale.getDefault(),
                        "%.2f",
                        viewModel.progressValue.floatValue * 100f
                    ) + "%",
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}

@ThemePreviews
@Composable
fun NfcDialogPreview() {
    val ctx = LocalContext.current
    BasePreview {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                NfcDialog(viewModel = CieSdkMethodsViewModel(CieSDK.withContext(ctx)).apply {
                    this.showDialog.value = true
                    this.progressValue.floatValue = 0.2f
                })
            }
        }
    }
}