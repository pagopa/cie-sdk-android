package it.pagopa.io.app.cie.pace.evp

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import javax.crypto.KeyAgreement
import javax.crypto.interfaces.DHPrivateKey
import javax.crypto.interfaces.DHPublicKey
import javax.crypto.spec.DHParameterSpec
import javax.crypto.spec.DHPublicKeySpec

internal class EvpDh(
    keyPair: KeyPair? = null,
    publicKey: PublicKey? = null
) : EvpKeyPair(keyPair, publicKey, KeyType.DH) {

    companion object {
        fun fromPubKeyData(pubKeyData: ByteArray, params: DHParameterSpec): EvpDh? {
            return try {
                val y = BigInteger(1, pubKeyData)
                val pubKeySpec = DHPublicKeySpec(y, params.p, params.g)
                val kf = KeyFactory.getInstance("DH", BouncyCastleProvider.PROVIDER_NAME)
                val pubKey = kf.generatePublic(pubKeySpec)
                EvpDh(null, pubKey)
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun getPublicKeyData(): ByteArray? {
        val pub = (keyPair?.public ?: publicKey) as? DHPublicKey ?: return null
        val pLen = (pub.params.p.bitLength() + 7) / 8
        val yBytes = pub.y.toByteArray()
        return when {
            yBytes.size == pLen -> yBytes
            yBytes.size < pLen -> ByteArray(pLen - yBytes.size) + yBytes
            else -> yBytes.copyOfRange(yBytes.size - pLen, yBytes.size)
        }
    }

    override fun computeSharedSecret(peerPublicKey: EvpKeyPair): ByteArray? {
        val priv = keyPair?.private as? DHPrivateKey ?: return null
        val peerPub = (peerPublicKey.keyPair?.public ?: peerPublicKey.publicKey) as? DHPublicKey ?: return null
        val ka = KeyAgreement.getInstance("DH", BouncyCastleProvider.PROVIDER_NAME)
        ka.init(priv)
        ka.doPhase(peerPub, true)
        return ka.generateSecret()
    }

    /**
     * Implementa il PACE Generic Mapping per DH:
     * g' = (g^nonce mod p) * h mod p
     * dove h = ciePublicKey^privKey mod p
     */
    override fun doMappingAgreement(ciePublicKeyData: ByteArray, nonce: BigInteger): EvpDh {
        val pub = keyPair?.public as DHPublicKey
        val params = pub.params
        val p = params.p
        val g = params.g
        // PACE Generic Mapping: g' = g^nonce mod p
        val gPrime = g.modPow(nonce, p)
        val newParams = DHParameterSpec(p, gPrime, params.l)
        val keyGen = KeyPairGenerator.getInstance("DH", BouncyCastleProvider.PROVIDER_NAME)
        keyGen.initialize(newParams)
        val newKeyPair = keyGen.generateKeyPair()
        return EvpDh(newKeyPair, null)
    }
}