package it.pagopa.io.app.cie.pace.evp

import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.nfc.Utils
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
        val peerPub = (peerPublicKey.keyPair?.public ?: peerPublicKey.publicKey) as? DHPublicKey
            ?: return null
        val ka = KeyAgreement.getInstance("DH", BouncyCastleProvider.PROVIDER_NAME)
        ka.init(priv)
        CieLogger.i(
            "PACE-SHARED",
            "My ephemeral private key x: ${(keyPair?.private as DHPrivateKey).x.toString(16)}"
        )
        CieLogger.i("PACE-SHARED", "Peer ephemeral public key y: ${peerPub.y.toString(16)}")
        ka.doPhase(peerPub, true)
        val secret = ka.generateSecret()
        CieLogger.i(
            "PACE-SHARED",
            "Shared Secret (${secret.size} bytes): ${Utils.bytesToString(secret)}"
        )
        return secret
    }

    /**
     * Implementa il PACE Generic Mapping per DH:
     * g' = (g^nonce mod p) * h mod p
     * dove h = ciePublicKey^privKey mod p
     */
    override fun doMappingAgreement(ciePublicKeyData: ByteArray, nonce: BigInteger): EvpDh {
        val priv = keyPair?.private as DHPrivateKey
        val pub = keyPair?.public as DHPublicKey
        val params = pub.params
        val p = params.p
        val g = params.g

        CieLogger.i("PACE-MAP-DH", "p (${p.bitLength()} bits): ${p.toString(16)}")
        CieLogger.i("PACE-MAP-DH", "g: ${g.toString(16)}")
        CieLogger.i("PACE-MAP-DH", "nonce: ${nonce.toString(16)}")
        CieLogger.i("PACE-MAP-DH", "priv.x: ${priv.x.toString(16)}")

        val ciePubY = BigInteger(1, ciePublicKeyData)
        CieLogger.i("PACE-MAP-DH", "CIE Mapping Public Key Y: ${ciePubY.toString(16)}")

        // h = ciePubY^priv.x mod p
        val h = ciePubY.modPow(priv.x, p)
        CieLogger.i("PACE-MAP-DH", "h: ${h.toString(16)}")

        // g' = (g^nonce mod p) * h mod p
        val gPrime = g.modPow(nonce, p).multiply(h).mod(p)
        CieLogger.i("PACE-MAP-DH", "gPrime: ${gPrime.toString(16)}")

        val newParams = DHParameterSpec(p, gPrime, params.l)
        val keyGen = KeyPairGenerator.getInstance("DH", BouncyCastleProvider.PROVIDER_NAME)
        keyGen.initialize(newParams)
        val newKeyPair = keyGen.generateKeyPair()

        val ephPub = newKeyPair.public as DHPublicKey
        CieLogger.i("PACE-MAP-DH", "My Ephemeral PubKey: ${ephPub.y.toString(16)}")

        return EvpDh(newKeyPair, null)
    }
}