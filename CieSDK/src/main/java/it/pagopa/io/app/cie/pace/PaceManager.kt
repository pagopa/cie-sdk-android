package it.pagopa.io.app.cie.pace

import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.cie.CieSdkException
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.cie.OnTransmit
import it.pagopa.io.app.cie.cie.commands.CieCommands
import it.pagopa.io.app.cie.nfc.Utils
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

        // 2️⃣ EF.CardAccess READ BINARY complete
        val efData = readEfCardAccess()
        if (CieLogger.enabled) {
            CieLogger.i("EF.CardAccess HEX", Utils.bytesToString(efData))
            listAllOidsFromCardAccess(efData)
        }
        // 3️⃣ Parsing OID PACE
        val paceInfos = PACEInfo.fromCardAccess(efData)
        val back = paceInfos.firstOrNull { pace ->
            val oid = pace.objIdentifier()
            oid != null && oid.keyLength()!! < 256 // evita DH 2048
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
        val resp = this.generalAuthenticateStep0()
        resp.parseResponse(NfcEvent.GENERAL_AUTHENTICATE_STEP0)
        val parser = TlvReader(resp.response)
        val encryptedNonceContainer = parser.readAll().firstOrNull()?.value
        if (encryptedNonceContainer == null)
            throw CieSdkException(NfcError.ENCRYPTED_NONCE_NOT_FOUND)
        CieLogger.i("encryptedNonceContainer", Utils.bytesToString(encryptedNonceContainer))
        val parserContainer = TlvReader(encryptedNonceContainer)
        val encryptedNonce = parserContainer.readAll().firstOrNull()?.value
        if (encryptedNonce == null)
            throw CieSdkException(NfcError.ENCRYPTED_NONCE_NOT_FOUND)
        CieLogger.i("EncryptedNonce", Utils.bytesToString(encryptedNonce))
        CieLogger.i("PACE", "decrypting NONCE;\npaceKeY: ${Utils.bytesToString(paceKey)}")
        val decryptedNonce = PaceDecrypt().decryptNonce(cipherAlgName, paceKey, encryptedNonce)
        CieLogger.i("decryptedNonce", Utils.bytesToString(decryptedNonce))
        return decryptedNonce
    }

    @Throws(Exception::class)
    fun doPACE(can: String): Triple<ByteArray, ByteArray, ByteArray> {
        val commands = CieCommands(onTransmit)
        commands.selectPace()
        val paceInfo = commands.getOidForPace()
        if (paceInfo == null || paceInfo.parameterId == null)
            throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                this.msg = "PACE OID not found"
            })
        // 2️⃣ MSE:Set AT (PACE, brainpoolP256r1, CAN)
        var resp = commands.setMsePaceCan(paceInfo.paceRawValue)
        resp.parseResponse(NfcEvent.SET_MSE)
        val paceOid = paceInfo.objIdentifier()!!
        CieLogger.i("PACE OID", paceOid.name)
        // if there is a paceOid will never be null
        val (cipherAlgName, digestAlgo) = paceOid.kindOfObjId()!!
        CieLogger.i("CipherAlgName", cipherAlgName.name)
        CieLogger.i("digestAlgo", digestAlgo.name)
        val paceKey = deriveKey(
            keySeed = can.toByteArray(Charsets.UTF_8),
            cipherAlgName = cipherAlgName,
            digestAlgo = digestAlgo,
            keyLength = paceOid.keyLength()!!,
            nonce = null,
            mode = SecureMessagingMode.PACE_MODE
        )
        val decryptedNonce = commands.stepOne(cipherAlgName, paceKey)
        //END OF STEP 1
        // GA Phase 1
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
        resp = commands.generalAuthenticateStep1(rawPublicKeyVal)
        resp.parseResponse(NfcEvent.GENERAL_AUTHENTICATE_STEP1)
        val ciePublicKeyByteContainer = resp.response
        val reader = TlvReader(ciePublicKeyByteContainer)
        val tlvs = reader.readRaw()
        val container = tlvs.firstOrNull {
            it.tag == 0x7c
        }
        if (container == null)
            throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                this.msg = "Unable to get public key from CIE"
            })
        val readerPublicKey = TlvReader(container.value)
        val publicKeyTLv = readerPublicKey.readRaw().firstOrNull {
            it.tag == 0x82
        }
        if (publicKeyTLv == null)
            throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                this.msg = "Unable to get public key from CIE"
            })
        val ciePublicKey = publicKeyTLv.value
        CieLogger.i("C.I.E. Public Key tlv", publicKeyTLv.toString())
        CieLogger.i("C.I.E. Public Key cleaned", Utils.bytesToString(ciePublicKey))

        resp = commands.generalAuthenticateStep2(ciePublicKey)
        resp.parseResponse(NfcEvent.GENERAL_AUTHENTICATE_STEP2)

        return Triple(byteArrayOf(), byteArrayOf(), byteArrayOf())
    }
}