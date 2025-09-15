package it.pagopa.io.app.cie.pace.general_authenticate

import it.pagopa.io.app.cie.cie.CieSdkException
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.cie.commands.CieCommands
import it.pagopa.io.app.cie.pace.KeyAgreementAlgorithm
import it.pagopa.io.app.cie.pace.SecureMessagingMode
import it.pagopa.io.app.cie.pace.Tlv
import it.pagopa.io.app.cie.pace.TlvReader
import it.pagopa.io.app.cie.pace.deriveKey
import it.pagopa.io.app.cie.pace.evp.EvpDh
import it.pagopa.io.app.cie.pace.evp.EvpEc
import it.pagopa.io.app.cie.pace.evp.EvpKeyPair
import it.pagopa.io.app.cie.pace.general_authenticate.model.Phase1Model
import it.pagopa.io.app.cie.pace.general_authenticate.model.Phase2Model
import it.pagopa.io.app.cie.pace.general_authenticate.model.PhaseModel
import it.pagopa.io.app.cie.pace.utils.generalAuthenticateStep2
import it.pagopa.io.app.cie.pace.utils.offsetToBigInt
import it.pagopa.io.app.cie.pace.utils.parse7c82Tlv
import it.pagopa.io.app.cie.pace.utils.parseResponse
import java.security.interfaces.ECPublicKey
import javax.crypto.interfaces.DHPublicKey

internal class Phase2(commands: CieCommands) : Phase(commands) {
    private fun CieCommands.gaPhaseTwo(rawPublicKeyVal: ByteArray): Tlv {
        // Send GENERAL AUTHENTICATE (phase 2) with ephemeral public key
        val resp = this.generalAuthenticateStep2(rawPublicKeyVal)
        resp.parseResponse(NfcEvent.GENERAL_AUTHENTICATE_STEP2)

        // Parse CIE ephemeral public key from response
        val ciePublicKeyByteContainer = resp.response
        return TlvReader(ciePublicKeyByteContainer).parse7c82Tlv()
    }

    override fun <T> execute(input: T): PhaseModel {
        val model = input as Phase1Model
        val kindOfAlgorithm = model.kindOfAlgorithm
        val keyPair = model.keyPair
        val cieMappingPublicKey = model.ciePublicKeyMapping.value
        val decryptedNonce = model.decryptedNonce
        val paceOid = model.paceOid
        // Create EvpKeyPair from mapping key pair
        val evpKeyPair = when (kindOfAlgorithm) {
            KeyAgreementAlgorithm.DH -> EvpDh(keyPair, null)
            KeyAgreementAlgorithm.ECDH -> EvpEc(keyPair, null)
        }

        // Perform mapping agreement to obtain ephemeral key pair
        val ephemeralKey = evpKeyPair.doMappingAgreement(
            cieMappingPublicKey,
            decryptedNonce.offsetToBigInt()
        )

        // My ephemeral public key (GA Phase 2)
        val publicKeyBytes = ephemeralKey.getPublicKeyData()!!

        // Receive CIE ephemeral public key
        val cieEphemeralPublicKey = commands.gaPhaseTwo(publicKeyBytes)

        // Build CIE ephemeral key pair object
        val cieEphemeralKeyPair = EvpKeyPair.from(
            pubKeyData = cieEphemeralPublicKey.value,
            params = ephemeralKey.keyPair!!.public.let {
                when (kindOfAlgorithm) {
                    KeyAgreementAlgorithm.DH -> (it as DHPublicKey).params
                    KeyAgreementAlgorithm.ECDH -> (it as ECPublicKey).params
                }
            },
            keyType = ephemeralKey.keyType
        )

        // Compute shared secret
        val sharedSecret = ephemeralKey.computeSharedSecret(cieEphemeralKeyPair)
            ?: throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                msg = "Shared secret not computed"
            })

        // Derive ENC and MAC keys
        val keyLength = paceOid.keyLength()!!
        val (cipherAlgName, digestAlgo) = paceOid.kindOfObjId()!!
        val encKey = deriveKey(
            sharedSecret,
            cipherAlgName,
            digestAlgo,
            keyLength,
            null,
            SecureMessagingMode.ENC_MODE
        )
        val macKey = deriveKey(
            sharedSecret,
            cipherAlgName,
            digestAlgo,
            keyLength,
            null,
            SecureMessagingMode.MAC_MODE
        )
        return Phase2Model(
            cieEphemeralPublicKey = cieEphemeralPublicKey,
            macKey = macKey,
            encKey = encKey,
            paceOid = paceOid,
            publicKeyBytes = publicKeyBytes,
            cipherAlgName = cipherAlgName
        )
    }
}