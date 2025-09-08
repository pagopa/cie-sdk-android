package it.pagopa.io.app.cie.pace

import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.cie.CieSdkException
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.cie.OnTransmit
import it.pagopa.io.app.cie.cie.commands.CieCommands
import it.pagopa.io.app.cie.nfc.Utils
import it.pagopa.io.app.cie.pace.evp.EvpDh
import it.pagopa.io.app.cie.pace.evp.EvpEc
import it.pagopa.io.app.cie.pace.evp.EvpKeyPair
import it.pagopa.io.app.cie.pace.pace_model.PACEInfo
import it.pagopa.io.app.cie.pace.utils.addBcIfNeeded
import it.pagopa.io.app.cie.pace.utils.generalAuthenticateStep0
import it.pagopa.io.app.cie.pace.utils.generalAuthenticateStep1
import it.pagopa.io.app.cie.pace.utils.generalAuthenticateStep2
import it.pagopa.io.app.cie.pace.utils.listAllOidsFromCardAccess
import it.pagopa.io.app.cie.pace.utils.offsetToBigInt
import it.pagopa.io.app.cie.pace.utils.parseResponse
import it.pagopa.io.app.cie.pace.utils.readEfCardAccess
import it.pagopa.io.app.cie.pace.utils.selectEfCardAccess
import it.pagopa.io.app.cie.pace.utils.selectPace
import it.pagopa.io.app.cie.pace.utils.sendGeneralAuthenticateToken
import it.pagopa.io.app.cie.pace.utils.setMsePaceCan
import java.security.interfaces.ECPublicKey
import javax.crypto.interfaces.DHPublicKey

internal class PaceManager(private val onTransmit: OnTransmit) {
    init {
        addBcIfNeeded()
    }

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

    private fun TlvReader.parse7c82Tlv(): Tlv {
        // Parse outer TLV container (tag 0x7C)
        val tlvs = this.readRaw()
        val container = tlvs.firstOrNull {
            it.tag == 0x7c
        }
        if (container == null) {
            CieLogger.e("parse7c82Tlv", "Unable to get public key from CIE")
            throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                this.msg = "Unable to get public key from CIE"
            })
        }

        // Parse inner TLVs from container
        val readerPublicKeyTlvs = TlvReader(container.value).readAll()
        readerPublicKeyTlvs.forEach {
            CieLogger.i("TLV", it.toString())
        }

        // Search for public key TLV tag (0x82 or 0x84)
        val publicKeyTLv = readerPublicKeyTlvs.firstOrNull {
            it.tag == 0x82 || it.tag == 0x83 || it.tag == 0x84
        }
        if (publicKeyTLv == null) {
            CieLogger.e("parse7c82Tlv", "Unable to get public key from container")
            throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                this.msg = "Unable to get public key from CIE from container"
            })
        }
        return publicKeyTLv
    }

    private fun CieCommands.gaPhaseOne(rawPublicKeyVal: ByteArray): Tlv {
        // Send GENERAL AUTHENTICATE (phase 1) with mapping public key
        val resp = this.generalAuthenticateStep1(rawPublicKeyVal)
        resp.parseResponse(NfcEvent.GENERAL_AUTHENTICATE_STEP1)

        // Parse CIE mapping public key from response
        val ciePublicKeyByteContainer = resp.response
        return TlvReader(ciePublicKeyByteContainer).parse7c82Tlv()
    }

    private fun CieCommands.gaPhaseTwo(rawPublicKeyVal: ByteArray): Tlv {
        // Send GENERAL AUTHENTICATE (phase 2) with ephemeral public key
        val resp = this.generalAuthenticateStep2(rawPublicKeyVal)
        resp.parseResponse(NfcEvent.GENERAL_AUTHENTICATE_STEP2)

        // Parse CIE ephemeral public key from response
        val ciePublicKeyByteContainer = resp.response
        return TlvReader(ciePublicKeyByteContainer).parse7c82Tlv()
    }

    @Throws(Exception::class)
    fun doPACE(can: String): Pair<ByteArray, ByteArray> {
        val commands = CieCommands(onTransmit)

        CieLogger.i("PACE-DEBUG", "=== START doPACE ===")
        CieLogger.i("PACE-DEBUG", "CAN: $can")

        // 2. Retrieve PACE OID and parameters
        val paceInfo = commands.getOidForPace()
            ?: throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                msg = "PACE OID not found"
            })

        // 3. Set MSE:AT for PACE with CAN
        commands.setMsePaceCan(paceInfo.paceRawValue).parseResponse(NfcEvent.SET_MSE)

        val paceOid = paceInfo.objIdentifier()!!
        val (cipherAlgName, digestAlgo) = paceOid.kindOfObjId()!!

        // 4. Derive paceKey from CAN
        val paceKey = deriveKey(
            keySeed = can.toByteArray(Charsets.UTF_8),
            cipherAlgName = cipherAlgName,
            digestAlgo = digestAlgo,
            keyLength = paceOid.keyLength()!!,
            nonce = null,
            mode = SecureMessagingMode.PACE_MODE
        )

        // 5. Step 1: Get and decrypt nonce
        val decryptedNonce = commands.stepOne(cipherAlgName, paceKey)

        // 6. Step 2: Mapping key agreement
        val kindOfAlgorithm = paceOid.keyAgreementAlgorithm()!!
        val mappingKey = MappingKey()
        val keyPair = mappingKey.createMappingKey(
            kindOfAlgorithm,
            PACEDomainParam.fromId(paceInfo.parameterId!!)!!
        )

        // My mapping public key (GA Phase 1)
        val rawPublicKeyVal = mappingKey.toRawData(
            when (kindOfAlgorithm) {
                KeyAgreementAlgorithm.DH -> keyPair.public as DHPublicKey
                KeyAgreementAlgorithm.ECDH -> keyPair.public as ECPublicKey
            }
        ) ?: throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
            msg = "Public Key not found"
        })

        val cieMappingPublicKey = commands.gaPhaseOne(rawPublicKeyVal).value

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

        // 7. Compute shared secret
        val sharedSecret = ephemeralKey.computeSharedSecret(cieEphemeralKeyPair)
            ?: throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                msg = "Shared secret not computed"
            })

        // 8. Derive ENC and MAC keys
        val keyLength = paceOid.keyLength()!!
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

        // --- Mutual authentication ---
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

        CieLogger.i("PACE-DEBUG", "Mutual authentication OK - PACE completed successfully!")
        CieLogger.i("PACE-DEBUG", "=== END doPACE ===")

        // Return keys: sharedSecret, ENC key, MAC key
        return encKey to macKey
    }
}