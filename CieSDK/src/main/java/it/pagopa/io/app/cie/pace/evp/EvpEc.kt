package it.pagopa.io.app.cie.pace.evp

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.math.ec.ECCurve
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECFieldFp
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.EllipticCurve
import javax.crypto.KeyAgreement

internal class EvpEc(
    keyPair: KeyPair? = null,
    publicKey: PublicKey? = null
) : EvpKeyPair(keyPair, publicKey, KeyType.EC) {

    companion object {
        fun fromPubKeyData(pubKeyData: ByteArray, params: ECParameterSpec): EvpEc? {
            return try {
                val ecPoint = decodeECPoint(pubKeyData, params.curve)
                val pubKeySpec = ECPublicKeySpec(ecPoint, params)
                val kf = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
                val pubKey = kf.generatePublic(pubKeySpec)
                EvpEc(null, pubKey)
            } catch (e: Exception) {
                null
            }
        }

        private fun decodeECPoint(data: ByteArray, curve: EllipticCurve): ECPoint {
            require(data[0] == 0x04.toByte()) { "Only uncompressed points supported" }
            val fieldSize = (curve.field.fieldSize + 7) / 8
            val x = BigInteger(1, data.copyOfRange(1, 1 + fieldSize))
            val y = BigInteger(1, data.copyOfRange(1 + fieldSize, 1 + 2 * fieldSize))
            return ECPoint(x, y)
        }
    }

    override fun getPublicKeyData(): ByteArray? {
        val pub = (keyPair?.public ?: publicKey) as? ECPublicKey ?: return null
        val fieldSize = (pub.params.curve.field.fieldSize + 7) / 8
        val xBytes = toFixedLengthUnsigned(pub.w.affineX.toByteArray(), fieldSize)
        val yBytes = toFixedLengthUnsigned(pub.w.affineY.toByteArray(), fieldSize)
        return byteArrayOf(0x04) + xBytes + yBytes
    }

    override fun computeSharedSecret(peerPublicKey: EvpKeyPair): ByteArray? {
        val priv = keyPair?.private as? ECPrivateKey ?: return null
        val peerPub = (peerPublicKey.keyPair?.public ?: peerPublicKey.publicKey) as? ECPublicKey
            ?: return null
        val ka = KeyAgreement.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
        ka.init(priv)
        ka.doPhase(peerPub, true)
        return ka.generateSecret()
    }

    /**
     * Implementa il PACE Generic Mapping per ECC:
     * Calcola il nuovo generatore G' = (G * nonce) + (PK_C * 1)
     */
    override fun doMappingAgreement(ciePublicKeyData: ByteArray, nonce: BigInteger): EvpEc {
        val pub = (keyPair?.public ?: publicKey) as ECPublicKey
        val ecParams = pub.params

        // PACE Generic Mapping: G' = nonce * G
        val gPrime = multiplyECPoint(ecParams.generator, nonce, ecParams.curve)

        val newParams = ECParameterSpec(
            ecParams.curve,
            gPrime,
            ecParams.order,
            ecParams.cofactor
        )

        val keyGen = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        keyGen.initialize(newParams)
        val newKeyPair = keyGen.generateKeyPair()

        return EvpEc(newKeyPair, null)
    }

    /**
     * Moltiplica la chiave pubblica della CIE per la chiave privata locale → ECPoint
     */
    private fun computeECDHMappingKeyPoint(
        privateKey: ECPrivateKey,
        inputKey: ByteArray,
        ecParams: ECParameterSpec
    ): org.bouncycastle.math.ec.ECPoint {
        val bcCurve = ECCurve.Fp(
            (ecParams.curve.field as ECFieldFp).p,
            ecParams.curve.a,
            ecParams.curve.b
        )
        val peerPoint = bcCurve.decodePoint(inputKey)
        return peerPoint.multiply(privateKey.s).normalize()
    }

    private fun toFixedLengthUnsigned(bytes: ByteArray, length: Int): ByteArray {
        val unsigned = if (bytes.isNotEmpty() && bytes[0] == 0.toByte()) {
            bytes.copyOfRange(1, bytes.size)
        } else {
            bytes
        }
        return if (unsigned.size < length) {
            ByteArray(length - unsigned.size) + unsigned
        } else if (unsigned.size > length) {
            unsigned.copyOfRange(unsigned.size - length, unsigned.size)
        } else {
            unsigned
        }
    }
}