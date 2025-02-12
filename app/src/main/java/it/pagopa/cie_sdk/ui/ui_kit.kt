package it.pagopa.cie_sdk.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.Companion
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import it.pagopa.cie_sdk.theme.CieSDKPocTheme
import it.pagopa.cie_sdk.ui.model.LazyButtonModel

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