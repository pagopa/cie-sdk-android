package it.pagopa.io.app.cie.pace.evp

import it.pagopa.io.app.cie.pace.utils.toFixedLengthUnsigned
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.math.ec.ECCurve
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.Security
import java.security.interfaces.ECPublicKey
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.ECFieldFp
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.EllipticCurve
import javax.crypto.KeyAgreement
import javax.crypto.interfaces.DHPublicKey
import javax.crypto.spec.DHParameterSpec
import javax.crypto.spec.DHPublicKeySpec

internal open class EvpKeyPair(
    var keyPair: KeyPair? = null,
    var publicKey: PublicKey? = null,
    var keyType: KeyType
) {
    enum class KeyType { DH, EC }
    companion object {

        init {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }
        }

        @Throws(Exception::class)
        fun fromParams(params: AlgorithmParameterSpec, keyType: KeyType): EvpKeyPair {
            val keyGen = when (keyType) {
                KeyType.DH -> KeyPairGenerator.getInstance("DH", BouncyCastleProvider.PROVIDER_NAME)
                KeyType.EC -> KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
            }
            keyGen.initialize(params)
            val keyPair = keyGen.generateKeyPair()
            return EvpKeyPair(keyPair, null, keyType)
        }

        @Throws(Exception::class)
        fun from(
            pubKeyData: ByteArray,
            params: AlgorithmParameterSpec,
            keyType: KeyType
        ): EvpKeyPair {
            val keyFactory = when (keyType) {
                KeyType.DH -> KeyFactory.getInstance("DH", BouncyCastleProvider.PROVIDER_NAME)
                KeyType.EC -> KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
            }
            val pubKeySpec = when (keyType) {
                KeyType.DH -> DHPublicKeySpec(
                    BigInteger(1, pubKeyData),
                    (params as DHParameterSpec).p, params.g
                )

                KeyType.EC -> ECPublicKeySpec(
                    decodeECPoint(pubKeyData, (params as ECParameterSpec).curve),
                    params
                )
            }
            val pubKey = keyFactory.generatePublic(pubKeySpec)
            return EvpKeyPair(null, pubKey, keyType)
        }

        private fun decodeECPoint(data: ByteArray, curve: EllipticCurve): ECPoint {
            require(data[0] == 0x04.toByte()) { "Only uncompressed points supported" }
            val fieldSize = (curve.field.fieldSize + 7) / 8
            val x = BigInteger(1, data.copyOfRange(1, 1 + fieldSize))
            val y = BigInteger(1, data.copyOfRange(1 + fieldSize, 1 + 2 * fieldSize))
            return ECPoint(x, y)
        }
    }

    open fun getPublicKeyData(): ByteArray? {
        return when (keyType) {
            KeyType.DH -> {
                val pub =
                    if (keyPair != null) keyPair!!.public as DHPublicKey else publicKey as DHPublicKey
                val pLen = (pub.params.p.bitLength() + 7) / 8
                val yBytes = pub.y.toByteArray()
                if (yBytes.size == pLen) yBytes
                else if (yBytes.size < pLen) ByteArray(pLen - yBytes.size) + yBytes
                else yBytes.copyOfRange(yBytes.size - pLen, yBytes.size)
            }

            KeyType.EC -> {
                val pub =
                    if (keyPair != null) keyPair!!.public as ECPublicKey else publicKey as ECPublicKey
                val fieldSize = (pub.params.curve.field.fieldSize + 7) / 8
                val xBytes = toFixedLengthUnsigned(pub.w.affineX.toByteArray(), fieldSize)
                val yBytes = toFixedLengthUnsigned(pub.w.affineY.toByteArray(), fieldSize)
                byteArrayOf(0x04) + xBytes + yBytes
            }
        }
    }

    open fun computeSharedSecret(peerPublicKey: EvpKeyPair): ByteArray? {
        val priv = keyPair?.private ?: return null
        val ka = when (keyType) {
            KeyType.DH -> KeyAgreement.getInstance("DH", BouncyCastleProvider.PROVIDER_NAME)
            KeyType.EC -> KeyAgreement.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
        }
        ka.init(priv)
        ka.doPhase(peerPublicKey.publicKey ?: peerPublicKey.keyPair!!.public, true)
        return ka.generateSecret()
    }

    open fun doMappingAgreement(ciePublicKeyData: ByteArray, nonce: BigInteger): EvpKeyPair {
        return when (keyType) {
            KeyType.DH -> {
                val pub = keyPair!!.public as DHPublicKey
                val p = pub.params.p
                val g = pub.params.g
                val l = pub.params.l
                val gPrime = g.modPow(nonce, p)
                val newParams = DHParameterSpec(p, gPrime, l)
                fromParams(newParams, KeyType.DH)
            }

            KeyType.EC -> {
                val pub = keyPair!!.public as ECPublicKey
                val ecParams = pub.params
                val gPrime = multiplyECPoint(ecParams.generator, nonce, ecParams.curve)
                val newSpec =
                    ECParameterSpec(ecParams.curve, gPrime, ecParams.order, ecParams.cofactor)
                fromParams(newSpec, KeyType.EC)
            }
        }
    }

    fun free() {
        keyPair = null
        publicKey = null
    }

    private fun multiplyECPoint(base: ECPoint, scalar: BigInteger, curve: EllipticCurve): ECPoint {
        val fieldSize = (curve.field.fieldSize + 7) / 8
        val xBytes = toFixedLengthUnsigned(base.affineX.toByteArray(), fieldSize)
        val yBytes = toFixedLengthUnsigned(base.affineY.toByteArray(), fieldSize)
        val bcCurve = ECCurve.Fp(
            (curve.field as ECFieldFp).p,
            curve.a,
            curve.b
        )
        val bcPoint = bcCurve.createPoint(BigInteger(1, xBytes), BigInteger(1, yBytes))
        val gPrime = bcPoint.multiply(scalar).normalize()
        return ECPoint(gPrime.affineXCoord.toBigInteger(), gPrime.affineYCoord.toBigInteger())
    }
}