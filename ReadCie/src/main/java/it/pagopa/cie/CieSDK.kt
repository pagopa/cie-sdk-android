package it.pagopa.cie

import android.content.Context
import android.nfc.NfcAdapter

class CieSDK {
    fun hasNfc(context: Context?) = NfcAdapter.getDefaultAdapter(context) != null
    fun isNfcAvailable(context: Context?) = NfcAdapter.getDefaultAdapter(context)?.isEnabled == true
}