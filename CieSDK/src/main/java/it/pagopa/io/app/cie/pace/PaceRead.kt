package it.pagopa.io.app.cie.pace

import it.pagopa.io.app.cie.nfc.Utils

data class PaceRead(val dg1: ByteArray, val dg11: ByteArray, val sod: ByteArray) {
    override fun toString(): String {
        val dg1 = Utils.bytesToString(this.dg1)
        val dg11 = Utils.bytesToString(this.dg11)
        val sod = Utils.bytesToString(this.sod)
        return "dg1:\n${dg1}\ndg11:\n${dg11}\nsod:\n${sod}"
    }
    fun toTerminalString():String{
        val dg1 = Utils.bytesToString(this.dg1)
        val dg11 = Utils.bytesToString(this.dg11)
        val sod = Utils.bytesToString(this.sod)
        return "dg1:\n\t${dg1}\ndg11:\n\t${dg11}\nsod:\n\t${sod}"
    }
}
