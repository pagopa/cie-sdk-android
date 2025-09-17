package it.pagopa.io.app.cie.pace.general_authenticate

import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.cie.CieSdkException
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.cie.commands.CieCommands
import it.pagopa.io.app.cie.nfc.Utils
import it.pagopa.io.app.cie.pace.KeyAgreementAlgorithm
import it.pagopa.io.app.cie.pace.MappingKey
import it.pagopa.io.app.cie.pace.PACECipherAlgorithms
import it.pagopa.io.app.cie.pace.PACEDomainParam
import it.pagopa.io.app.cie.pace.PaceDecrypt
import it.pagopa.io.app.cie.pace.SecureMessagingMode
import it.pagopa.io.app.cie.pace.Tlv
import it.pagopa.io.app.cie.pace.TlvReader
import it.pagopa.io.app.cie.pace.deriveKey
import it.pagopa.io.app.cie.pace.general_authenticate.model.Phase1Model
import it.pagopa.io.app.cie.pace.pace_model.PACEInfo
import it.pagopa.io.app.cie.pace.utils.generalAuthenticateStep0
import it.pagopa.io.app.cie.pace.utils.generalAuthenticateStep1
import it.pagopa.io.app.cie.pace.utils.listAllOidsFromCardAccess
import it.pagopa.io.app.cie.pace.utils.parse7c82Tlv
import it.pagopa.io.app.cie.pace.utils.parseResponse
import it.pagopa.io.app.cie.pace.utils.readEfCardAccess
import it.pagopa.io.app.cie.pace.utils.selectEfCardAccess
import it.pagopa.io.app.cie.pace.utils.setMsePaceCan
import java.security.interfaces.ECPublicKey
import javax.crypto.interfaces.DHPublicKey

internal class Phase1(commands: CieCommands) : Phase(commands) {
    private fun CieCommands.getOidForPace(): PACEInfo? {
        // 1️⃣ SELECT EF.CardAccess (FID 011C)
        val selResp = selectEfCardAccess()
        selResp.parseResponse(NfcEvent.SELECT_EF_CARDACCESS)

        // 2️⃣ Read EF.CardAccess file content completely
        val efData = readEfCardAccess()
        if (CieLogger.enabled) {
            CieLogger.i("EF.CardAccess HEX", Utils.bytesToString(efData))
            listAllOidsFromCardAccess(efData)
        }

        // 3️⃣ Parse PACE OID from EF.CardAccess
        val paceInfos = PACEInfo.fromCardAccess(efData)
        val back = paceInfos.firstOrNull { pace ->
            val oid = pace.objIdentifier()
            oid != null && oid.keyLength()!! < 256 // avoid DH 2048 keys
        }
        CieLogger.i(
            "CHOSEN OID PACE CAN",
            "${back?.paceValue.orEmpty()}\n${back?.parameterId}\n${back?.objIdentifier()?.name}"
        )
        return back
    }

    private fun CieCommands.gaPhaseOne(rawPublicKeyVal: ByteArray): Tlv {
        // Send GENERAL AUTHENTICATE (phase 1) with mapping public key
        val resp = this.generalAuthenticateStep1(rawPublicKeyVal)
        resp.parseResponse(NfcEvent.GENERAL_AUTHENTICATE_STEP1)

        // Parse CIE mapping public key from response
        val ciePublicKeyByteContainer = resp.response
        return TlvReader(ciePublicKeyByteContainer).parse7c82Tlv()
    }

    private fun CieCommands.stepOne(
        cipherAlgName: PACECipherAlgorithms,
        paceKey: ByteArray
    ): ByteArray {
        // First GENERAL AUTHENTICATE to get encrypted nonce
        val resp = this.generalAuthenticateStep0()
        resp.parseResponse(NfcEvent.GENERAL_AUTHENTICATE_STEP0)

        // Parse encrypted nonce from response
        val parser = TlvReader(resp.response)
        val encryptedNonceContainer = parser.readAll().firstOrNull()?.value
        if (encryptedNonceContainer == null)
            throw CieSdkException(NfcError.ENCRYPTED_NONCE_NOT_FOUND)

        CieLogger.i("encryptedNonceContainer", Utils.bytesToString(encryptedNonceContainer))

        // Extract inner encrypted nonce
        val parserContainer = TlvReader(encryptedNonceContainer)
        val encryptedNonce = parserContainer.readAll().firstOrNull()?.value
        if (encryptedNonce == null)
            throw CieSdkException(NfcError.ENCRYPTED_NONCE_NOT_FOUND)

        CieLogger.i("EncryptedNonce", Utils.bytesToString(encryptedNonce))
        CieLogger.i("PACE", "decrypting NONCE;\npaceKeY: ${Utils.bytesToString(paceKey)}")

        // Decrypt nonce using derived paceKey
        val decryptedNonce = PaceDecrypt().decryptNonce(cipherAlgName, paceKey, encryptedNonce)
        CieLogger.i("decryptedNonce", Utils.bytesToString(decryptedNonce))
        return decryptedNonce
    }

    override fun <T> execute(input: T): Phase1Model {
        val can = input as String
        // 1. Retrieve PACE OID and parameters
        val paceInfo = commands.getOidForPace()
            ?: throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                msg = "PACE OID not found"
            })
        // 2. Set MSE:AT for PACE with CAN
        commands.setMsePaceCan(paceInfo.paceRawValue).parseResponse(NfcEvent.SET_MSE)

        val paceOid = paceInfo.objIdentifier()!!
        val (cipherAlgName, digestAlgo) = paceOid.kindOfObjId()!!
        CieLogger.i("cipherAlgName - digestAlgo", "$cipherAlgName - $digestAlgo")
        // 3. Derive paceKey from CAN
        val paceKey = deriveKey(
            keySeed = can.toByteArray(Charsets.UTF_8),
            cipherAlgName = cipherAlgName,
            digestAlgo = digestAlgo,
            keyLength = paceOid.keyLength()!!,
            nonce = null,
            mode = SecureMessagingMode.PACE_MODE
        )
        CieLogger.i("pace key", Utils.bytesToString(paceKey))
        // 4. Step 1: Get and decrypt nonce
        val decryptedNonce = commands.stepOne(cipherAlgName, paceKey)

        // 5. Step 2: Mapping key agreement
        val kindOfAlgorithm = paceOid.keyAgreementAlgorithm()!!
        val mappingKey = MappingKey()
        val keyPair = mappingKey.createMappingKey(
            kindOfAlgorithm,
            PACEDomainParam.fromId(paceInfo.parameterId!!)!!
        )

        // My mapping public key (GA Phase 1 preparation)
        val rawPublicKeyVal = mappingKey.toRawData(
            when (kindOfAlgorithm) {
                KeyAgreementAlgorithm.DH -> keyPair.public as DHPublicKey
                KeyAgreementAlgorithm.ECDH -> keyPair.public as ECPublicKey
            }
        ) ?: throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
            msg = "Public Key not found"
        })

        val cieMappingPublicKeyTlv = commands.gaPhaseOne(rawPublicKeyVal)
        return Phase1Model(
            kindOfAlgorithm,
            keyPair,
            cieMappingPublicKeyTlv,
            decryptedNonce,
            paceOid
        )
    }
}