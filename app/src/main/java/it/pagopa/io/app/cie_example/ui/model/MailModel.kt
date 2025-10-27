package it.pagopa.io.app.cie_example.ui.model

data class MailModel(
    val recipients: List<String>,
    val subject: String,
    val body: String
)
