package it.pagopa.cie.cie

import android.util.Base64
import it.pagopa.cie.nfc.NfcReading
import it.pagopa.cie.nfc.Utils
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class ReadCie(private val onTransmit: OnTransmit, private val readingInterface: NfcReading) {
    fun read(challenge: String, itsOk: () -> Unit) {
        try {
            val commands = CieCommands(onTransmit)
            val efIntServ1001: ByteArray = commands.readNis()
            val nis = String(efIntServ1001, StandardCharsets.UTF_8)
            val bytes = commands.readPublicKey()
            val asn1Tag: Asn1Tag? = try {
                Asn1Tag.parse(bytes, false)
            } catch (_: Exception) {
                null
            }
            val a5noHash = if (asn1Tag != null) {
                Utils.bytesToString(MessageDigest.getInstance("SHA-256").digest(Utils.getLeft(bytes, asn1Tag.endPos.toInt())))
            } else
                ""
            val sod = commands.readSodFileCompleted()
            val challengeSigned = commands.intAuth(challenge)
            if (challengeSigned == null || challengeSigned.isEmpty()) {
                onTransmit.error("exception occurred: no challenge signed")
            } else {
                readingInterface.read(
                    NisAuthenticated(
                        nis, Base64.encodeToString(bytes, Base64.DEFAULT),
                        a5noHash,
                        Base64.encodeToString(sod, Base64.DEFAULT),
                        Base64.encodeToString(challengeSigned, Base64.DEFAULT)
                    )
                )
            }
            itsOk.invoke()
        } catch (e: Exception) {
            onTransmit.error("exception occurred: ${e.message}")
        }
    }
}