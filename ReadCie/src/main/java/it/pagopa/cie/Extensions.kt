package it.pagopa.cie

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context?.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}


fun String.hexStringToByteArray() =
    ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }


fun ByteArray.toHex() =
    this.joinToString(separator = "") { it.toInt().and(0xff).toString(16).padStart(2, '0') }