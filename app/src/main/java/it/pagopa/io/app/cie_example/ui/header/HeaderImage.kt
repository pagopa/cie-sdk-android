package it.pagopa.io.app.cie_example.ui.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import it.pagopa.io.app.cie_example.R
import it.pagopa.io.app.cie_example.navigation.Routes
import it.pagopa.io.app.cie_example.ui.AppDialog
import it.pagopa.io.app.cie_example.ui.AppTextField
import it.pagopa.io.app.cie_example.ui.UserInteraction

data class HeaderImage(
    val icon: ImageVector,
    val contentDescription: String,
    val onIconClick: UserInteraction
)

fun MutableState<HeaderImage?>.hide() {
    this.value = null
}

fun List<MutableState<HeaderImage?>>.hideAll() {
    this.map { it.hide() }
}


@Composable
fun MutableState<HeaderImage?>.BackArrowIcon(navController: NavController) {
    this.value = HeaderImage(
        Icons.AutoMirrored.Default.ArrowBack,
        stringResource(R.string.back),
        onIconClick = {
            navController.popBackStack()
        })
}

@Composable
fun MutableState<HeaderImage?>.HomeIcon(navController: NavController) {
    this.value = HeaderImage(
        Icons.Default.Home,
        stringResource(R.string.home),
        onIconClick = {
            navController.popBackStack(Routes.Home, false)
        })
}

@Composable
fun MutableState<HeaderImage?>.MailIcon(
    onClick: (Array<String>) -> Unit
) {
    val showRecipients = remember { mutableStateOf(false) }
    this.value = HeaderImage(
        Icons.Default.Mail,
        stringResource(R.string.home),
        onIconClick = {
            showRecipients.value = !showRecipients.value
        }
    )
    AppDialog(
        showRecipients,
        titleId = R.string.send_mail
    ) {
        MailRecipients {
            onClick.invoke(it)
        }
    }
}

@Composable
fun MailRecipients(onclickButton: (Array<String>) -> Unit) {
    val mail = remember { mutableStateOf("") }
    val mail1 = remember { mutableStateOf("") }
    val mail2 = remember { mutableStateOf("") }
    Column(
        Modifier
            .wrapContentSize()
            .padding(4.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppTextField(mail, stringResource(R.string.mail))
        Spacer(Modifier.height(4.dp))
        AppTextField(mail1, stringResource(R.string.mail))
        Spacer(Modifier.height(4.dp))
        AppTextField(mail2, stringResource(R.string.mail))
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                onclickButton.invoke(arrayOf(mail.value, mail1.value, mail2.value).filter {
                    it.isNotEmpty()
                }.toTypedArray())
            },
            enabled = arrayOf(mail.value, mail1.value, mail2.value).any {
                it.isNotEmpty()
            },
            modifier = Modifier.wrapContentSize()
        ) {
            Text(stringResource(R.string.send_mail))
        }
    }
}
