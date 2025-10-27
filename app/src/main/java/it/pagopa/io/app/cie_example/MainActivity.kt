package it.pagopa.io.app.cie_example

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.navigation.compose.rememberNavController
import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie_example.navigation.CieSdkNavHost
import it.pagopa.io.app.cie_example.theme.CieSDKPocTheme
import it.pagopa.io.app.cie_example.ui.ThemePreviews
import it.pagopa.io.app.cie_example.ui.header.HeaderImage
import it.pagopa.io.app.cie_example.ui.header.TopBar
import it.pagopa.io.app.cie_example.ui.model.MailModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CieLogger.enabled = BuildConfig.DEBUG
        enableEdgeToEdge()
        setContent {
            this.MainApp()
        }
    }

    fun sendMail(model: MailModel) {
        val recipientsString = model.recipients.joinToString(",")
        CieLogger.i("MAIL_DEBUG", "Recipients: $recipientsString")
        CieLogger.i("MAIL_DEBUG-SUBJECT", "Subject: ${model.subject}")
        CieLogger.i("MAIL_DEBUG-BODY", "Body: ${model.body}")
        if (model.recipients.isEmpty()) {
            Toast.makeText(this, "Nessun destinatario specificato.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            var mailtoStr = "mailto:${Uri.encode(recipientsString)}"
            val params = mutableListOf<String>()
            if (model.subject.isNotBlank())
                params.add("subject=${Uri.encode(model.subject)}")
            if (model.body.isNotBlank())
                params.add("body=${Uri.encode(model.body)}")
            if (params.isNotEmpty())
                mailtoStr += "?${params.joinToString("&")}"
            CieLogger.i("MAIL_DEBUG-URI", "Constructed mailto URI: $mailtoStr")
            val mailtoUri = mailtoStr.toUri()
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = mailtoUri
                putExtra(Intent.EXTRA_SUBJECT, model.subject)
                putExtra(Intent.EXTRA_TEXT, model.body)
            }
            startActivity(Intent.createChooser(intent, "Scegli un client email..."))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Nessun client email installato.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            CieLogger.e("MAIL_ERROR", "Errore durante l'invio dell'email: ${e.message}")
            Toast.makeText(this, "Errore durante la preparazione dell'email.", Toast.LENGTH_SHORT)
                .show()
        }
    }
}


@Composable
fun MainActivity?.MainApp() {
    CieSDKPocTheme {
        val headerImageLeft = remember { mutableStateOf<HeaderImage?>(null) }
        val headerImageRight = remember { mutableStateOf<HeaderImage?>(null) }
        val titleResId = remember { mutableIntStateOf(R.string.app_name) }
        val navController = rememberNavController()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopBar(
                    titleResId = titleResId.intValue,
                    imageLeft = headerImageLeft.value,
                    imageRight = headerImageRight.value
                )
            }
        ) { innerPadding ->
            this.CieSdkNavHost(
                navController,
                innerPadding,
                headerImageLeft,
                titleResId,
                headerImageRight
            )
        }
    }
}

@ThemePreviews
@Composable
fun MainAppPreview() {
    null.MainApp()
}