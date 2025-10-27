package it.pagopa.io.app.cie_example.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import it.pagopa.io.app.cie_example.R
import it.pagopa.io.app.cie_example.ui.model.LazyButtonModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDialog(
    shouldShowDialog: MutableState<Boolean>?,
    @StringRes titleId: Int,
    @StringRes descriptionId: Int?=null,
    @StringRes buttonText: Int?=null,
    btnAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    contentIn: @Composable ColumnScope.() -> Unit
) {
    if (shouldShowDialog?.value == true) {
        BasicAlertDialog(
            onDismissRequest = {
                shouldShowDialog.value = false
                onDismiss?.invoke()
            },
            properties = DialogProperties(),
            content = {
                Column(
                    Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.background,
                            RoundedCornerShape(16.dp)
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(titleId),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontStyle = FontStyle.Italic
                    )
                    Spacer(Modifier.height(16.dp))
                    this.contentIn()
                    descriptionId?.let {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(descriptionId),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    buttonText?.let{
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton(model = LazyButtonModel(buttonText) {
                            shouldShowDialog.value = false
                            btnAction?.invoke()
                        })
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        )
    }
}

@Composable
fun SimpleLoader(showDialog: MutableState<Boolean>) {
    if (showDialog.value) {
        Dialog(
            onDismissRequest = { showDialog.value = false },
            DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@ThemePreviews
@Composable
fun AppDialogPreview() {
    BasePreview {
        val showDialog = remember { mutableStateOf(true) }
        AppDialog(
            showDialog,
            R.string.read_cie_dialog_title,
            R.string.read_cie_dialog_description,
            R.string.ok
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
                        .align(Alignment.TopStart)
                )
                Image(
                    painter = painterResource(R.drawable.phone),
                    contentDescription = null,
                    modifier = Modifier
                        .width(64.dp)
                        .height(64.dp)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}