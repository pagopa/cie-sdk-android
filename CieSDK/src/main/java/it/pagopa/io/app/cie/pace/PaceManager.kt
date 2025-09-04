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
import it.pagopa.io.app.cie.pace.utils.parseResponse
import it.pagopa.io.app.cie.pace.utils.readEfCardAccess
import it.pagopa.io.app.cie.pace.utils.selectEfCardAccess
import it.pagopa.io.app.cie.pace.utils.selectPace
import it.pagopa.io.app.cie.pace.utils.sendGeneralAuthenticateToken
import it.pagopa.io.app.cie.pace.utils.setMsePaceCan
import java.math.BigInteger
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

    private fun normalizeDhPublicKey(pubKey: ByteArray, pLengthBytes: Int): ByteArray {
        return when {
            pubKey.size == pLengthBytes -> pubKey
            pubKey.size < pLengthBytes -> ByteArray(pLengthBytes - pubKey.size) + pubKey
            else -> pubKey.copyOfRange(pubKey.size - pLengthBytes, pubKey.size)
        }
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
    fun doPACE(can: String): Triple<ByteArray, ByteArray, ByteArray> {
        val commands = CieCommands(onTransmit)

        CieLogger.i("PACE-DEBUG", "=== START doPACE ===")
        CieLogger.i("PACE-DEBUG", "CAN: $can")

        // Select PACE application
        commands.selectPace()

        // Get PACE OID and parameters from EF.CardAccess
        val paceInfo = commands.getOidForPace()
        if (paceInfo == null || paceInfo.parameterId == null)
            throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                this.msg = "PACE OID not found"
            })

        CieLogger.i("PACE-DEBUG", "PACE parameterId: ${paceInfo.parameterId}")
        CieLogger.i("PACE-DEBUG", "PACE OID raw: ${Utils.bytesToString(paceInfo.paceRawValue)}")

        // Set MSE:AT command for PACE with CAN
        var resp = commands.setMsePaceCan(paceInfo.paceRawValue)
        resp.parseResponse(NfcEvent.SET_MSE)

        val paceOid = paceInfo.objIdentifier()!!
        CieLogger.i("PACE-DEBUG", "PACE OID name: ${paceOid.name}")
        CieLogger.i("PACE-DEBUG", "PACE OID identifier: ${paceOid.objIdentifier}")

        // Retrieve cipher and digest algorithms from OID
        val (cipherAlgName, digestAlgo) = paceOid.kindOfObjId()!!
        CieLogger.i("PACE-DEBUG", "CipherAlgName: ${cipherAlgName.name}")
        CieLogger.i("PACE-DEBUG", "DigestAlgo: ${digestAlgo.name}")

        // Derive paceKey from CAN
        val paceKey = deriveKey(
            keySeed = can.toByteArray(Charsets.UTF_8),
            cipherAlgName = cipherAlgName,
            digestAlgo = digestAlgo,
            keyLength = paceOid.keyLength()!!,
            nonce = null,
            mode = SecureMessagingMode.PACE_MODE
        )
        CieLogger.i("PACE-DEBUG", "PACE Key: ${Utils.bytesToString(paceKey)}")

        // Step 1: Get and decrypt nonce from CIE
        val decryptedNonce = commands.stepOne(cipherAlgName, paceKey)
        CieLogger.i("PACE-DEBUG", "Decrypted Nonce: ${Utils.bytesToString(decryptedNonce)}")

        // Step 2: GA Phase 1 - Mapping key agreement
        val kindOfAlgorithm = paceOid.keyAgreementAlgorithm()!!
        val mappingKey = MappingKey()
        val keyPair = mappingKey.createMappingKey(
            kindOfAlgorithm,
            PACEDomainParam.fromId(paceInfo.parameterId)!!
        )

        val publicKey = when (kindOfAlgorithm) {
            KeyAgreementAlgorithm.DH -> keyPair.public as DHPublicKey
            KeyAgreementAlgorithm.ECDH -> keyPair.public as ECPublicKey
        }
        val rawPublicKeyVal = mappingKey.toRawData(publicKey)
        if (rawPublicKeyVal == null)
            throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                this.msg = "Public Key not found"
            })

        CieLogger.i(
            "PACE-DEBUG",
            "My Mapping Public Key RAW: ${Utils.bytesToString(rawPublicKeyVal)} (${rawPublicKeyVal.size} bytes)"
        )

        // Send mapping public key to CIE and receive CIE mapping public key
        val publicKeyTLv = commands.gaPhaseOne(rawPublicKeyVal)
        val ciePublicKey = publicKeyTLv.value
        CieLogger.i(
            "PACE-DEBUG",
            "CIE Mapping Public Key RAW: ${Utils.bytesToString(ciePublicKey)} (${ciePublicKey.size} bytes)"
        )

        // Create EvpKeyPair from mapping key pair
        val evpKeyPair = when (kindOfAlgorithm) {
            KeyAgreementAlgorithm.DH -> EvpDh(keyPair, null)
            KeyAgreementAlgorithm.ECDH -> EvpEc(keyPair, null)
        }

        // Perform mapping agreement to obtain ephemeral key pair
        val ephemeralKey = evpKeyPair.doMappingAgreement(
            ciePublicKey,
            BigInteger(1, decryptedNonce)// forcing unsigend
        )

        // Extract my ephemeral public key and send to CIE (GA Phase 2)
        val publicKeyBytes = ephemeralKey.getPublicKeyData()!!
        CieLogger.i(
            "PACE-DEBUG",
            "My Ephemeral Public Key RAW: ${Utils.bytesToString(publicKeyBytes)} (${publicKeyBytes.size} bytes)"
        )

        val cieEphemeralPublicKey = commands.gaPhaseTwo(publicKeyBytes)
        CieLogger.i(
            "PACE-DEBUG",
            "CIE Ephemeral Public Key RAW: ${Utils.bytesToString(cieEphemeralPublicKey.value)} (${cieEphemeralPublicKey.value.size} bytes)"
        )

        // Create EvpKeyPair from CIE ephemeral public key
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

        // Compute shared secret using my ephemeral private key and CIE's ephemeral public key
        CieLogger.i("PACE-DEBUG", "Computing Shared Secret...")
        val sharedSecret = ephemeralKey.computeSharedSecret(cieEphemeralKeyPair)
            ?: throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                this.msg = "Shared secret not computed"
            })
        CieLogger.i(
            "PACE-DEBUG",
            "Shared Secret: ${Utils.bytesToString(sharedSecret)} (${sharedSecret.size} bytes)"
        )
        CieLogger.i("My Ephemeral Public Key len", "${publicKeyBytes.size}")
        CieLogger.i("cieEphemeralPublicKey len", "${cieEphemeralPublicKey.value.size}")
        // Derive ENC and MAC keys from shared secret
        val keyLength = paceOid.keyLength()
        val encKey = deriveKey(
            keySeed = sharedSecret,
            cipherAlgName = cipherAlgName,
            digestAlgo = digestAlgo,
            keyLength = keyLength!!,
            nonce = null,
            mode = SecureMessagingMode.ENC_MODE
        )
        val macKey = deriveKey(
            keySeed = sharedSecret,
            cipherAlgName = cipherAlgName,
            digestAlgo = digestAlgo,
            keyLength = keyLength,
            nonce = null,
            mode = SecureMessagingMode.MAC_MODE
        )
        CieLogger.i("PACE-DEBUG", "ENC Key: ${Utils.bytesToString(encKey)} (${encKey.size} bytes)")
        CieLogger.i("PACE-DEBUG", "MAC Key: ${Utils.bytesToString(macKey)} (${macKey.size} bytes)")

        // Generate authentication token for PCD using CIE's ephemeral public key
        val authToken = AuthToken()
        val oid = PACEDomainParam.fromId(paceInfo.parameterId)!!.toCurveOid()
        var pcdToken: ByteArray
        CieLogger.i("cieEphemeralPublicKey.value", Utils.bytesToString(cieEphemeralPublicKey.value))
        val ciePubKeyNormalized = MappingKey().toRawData(cieEphemeralKeyPair.publicKey!!)
            ?: throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                msg = "Unable to normalize CIE public key"
            })
        CieLogger.i("CIE PubKey Normalized", Utils.bytesToString(ciePubKeyNormalized))
        try {
            pcdToken = authToken.generateAuthenticationToken(
                publicKey = ciePubKeyNormalized,
                macKey = macKey,
                oid = oid,
                cipherAlg = cipherAlgName
            )
            CieLogger.i(
                "PACE-DEBUG",
                "PCD Token: ${Utils.bytesToString(pcdToken)} (${pcdToken.size} bytes)"
            )
        } catch (_: Exception) {
            throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                this.msg = "Unable to generate authentication token using public key"
            })
        }

        // Send PCD token to CIE (GA Phase 3)
        CieLogger.i("PACE-DEBUG", "Sending PCD Token to CIE...")
        val respAuth = commands.sendGeneralAuthenticateToken(pcdToken)
        CieLogger.i("PACE-DEBUG", "GA Step 3 SW: ${respAuth.swHex}")
        respAuth.parseResponse(NfcEvent.GENERAL_AUTHENTICATE_STEP3)

        // Extract CIE token (tag 0x86) from response
        val cieTokenTlv = TlvReader(respAuth.response).readAll().firstOrNull { it.tag == 0x86 }
            ?: throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                msg = "CIE authentication token not found"
            })
        CieLogger.i(
            "PACE-DEBUG",
            "CIE Token Received: ${Utils.bytesToString(cieTokenTlv.value)} (${cieTokenTlv.value.size} bytes)"
        )

        // Generate expected CIE token using my ephemeral public key
        val expectedCieToken = authToken.generateAuthenticationToken(
            publicKey = publicKeyBytes,
            macKey = macKey,
            oid = oid,
            cipherAlg = cipherAlgName
        )
        CieLogger.i(
            "PACE-DEBUG",
            "Expected CIE Token: ${Utils.bytesToString(expectedCieToken)} (${expectedCieToken.size} bytes)"
        )

        // Compare received CIE token with expected one
        if (!cieTokenTlv.value.contentEquals(expectedCieToken)) {
            CieLogger.e("PACE-DEBUG", "CIE authentication token mismatch!")
            throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                msg = "CIE authentication token mismatch"
            })
        }

        CieLogger.i("PACE-DEBUG", "Mutual authentication OK - PACE completed successfully!")
        CieLogger.i("PACE-DEBUG", "=== END doPACE ===")

        // Return keys: sharedSecret, ENC key, MAC key
        return Triple(sharedSecret, encKey, macKey)
    }
}