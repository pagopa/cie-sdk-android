package it.pagopa.io.app.cie.pace.general_authenticate

import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.cie.CieSdkException
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.cie.commands.CieCommands
import it.pagopa.io.app.cie.nfc.Utils
import it.pagopa.io.app.cie.pace.AuthToken
import it.pagopa.io.app.cie.pace.TlvReader
import it.pagopa.io.app.cie.pace.general_authenticate.model.Phase2Model
import it.pagopa.io.app.cie.pace.general_authenticate.model.Phase3Model
import it.pagopa.io.app.cie.pace.general_authenticate.model.PhaseModel
import it.pagopa.io.app.cie.pace.utils.parseResponse
import it.pagopa.io.app.cie.pace.utils.sendGeneralAuthenticateToken

/**Mutual Authentication*/
internal class Phase3(commands: CieCommands) : Phase(commands) {
    override fun <T> execute(input: T): PhaseModel {
        val phase2Model = input as Phase2Model
        val cieEphemeralPublicKey = phase2Model.cieEphemeralPublicKey
        val macKey = phase2Model.macKey
        val paceOid = phase2Model.paceOid
        val publicKeyBytes = phase2Model.publicKeyBytes
        val cipherAlgName = phase2Model.cipherAlgName
        val authToken = AuthToken()
        // 📌 According to BSI TR-03110:
        // Step A: PCD sends MAC over **its own** ephemeral public key (publicKeyBytes)
        CieLogger.i("PACE-DEBUG", "Generating PCD Token using MY ephemeral public key...")
        val pcdToken = authToken.generateAuthenticationToken(
            publicKey = cieEphemeralPublicKey.value,
            macKey = macKey,
            oid = paceOid,
            cipherAlg = cipherAlgName
        )
        CieLogger.i("PACE-DEBUG", "PCD Token: ${Utils.bytesToString(pcdToken)}")

        // Send PCD token to CIE (GA Step 3)
        val respAuth = commands.sendGeneralAuthenticateToken(pcdToken)
        respAuth.parseResponse(NfcEvent.GENERAL_AUTHENTICATE_STEP3)

        // Step B: CIE responds with MAC over **its own** ephemeral public key
        val cieTokenTlv = TlvReader(respAuth.response).readAll().firstOrNull { it.tag == 0x86 }
            ?: throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                msg = "CIE authentication token not found"
            })
        CieLogger.i("PACE-DEBUG", "CIE Token Received: ${Utils.bytesToString(cieTokenTlv.value)}")

        // Step C: Locally compute expected CIE token
        CieLogger.i("PACE-DEBUG", "Generating expected CIE Token using CIE ephemeral public key...")
        val expectedCieToken = authToken.generateAuthenticationToken(
            publicKey = publicKeyBytes,
            macKey = macKey,
            oid = paceOid,
            cipherAlg = cipherAlgName
        )
        CieLogger.i("PACE-DEBUG", "Expected CIE Token: ${Utils.bytesToString(expectedCieToken)}")

        // Step D: Compare received vs expected CIE token
        if (!cieTokenTlv.value.contentEquals(expectedCieToken)) {
            CieLogger.e("PACE-DEBUG", "CIE authentication token mismatch!")
            throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                msg = "CIE authentication token mismatch"
            })
        }
        //If all ok, we give back mac and enc keys
        return Phase3Model(
            macKey = macKey,
            encKey = phase2Model.encKey
        )
    }
}