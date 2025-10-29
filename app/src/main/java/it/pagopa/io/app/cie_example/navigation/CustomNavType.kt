package it.pagopa.io.app.cie_example.navigation

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie_example.ui.model.NisAndPaceReadDto
import it.pagopa.io.app.cie_example.ui.model.NisDto
import it.pagopa.io.app.cie_example.ui.model.PaceReadDto
import kotlinx.serialization.json.Json

object CustomNavType {
    private inline fun <reified T> toNavTypeObject(): NavType<T> {
        return object : NavType<T>(
            isNullableAllowed = false
        ) {
            override fun get(bundle: Bundle, key: String): T? {
                CieLogger.i("DECODING BUNDLE", bundle.toString())
                return Json.decodeFromString<T>(bundle.getString(key) ?: return null)
            }

            override fun parseValue(value: String): T {
                return Json.decodeFromString(Uri.decode(value))
            }

            override fun put(
                bundle: Bundle,
                key: String,
                value: T
            ) {
                bundle.putString(key, Json.encodeToString(value))
            }

            override fun serializeAsValue(value: T): String {
                val json = Json.encodeToString(value)
                return Uri.encode(json)
            }
        }
    }

    val paceReadType = this.toNavTypeObject<PaceReadDto>()
    val nisAndPaceReadType = this.toNavTypeObject<NisAndPaceReadDto>()
    val nisDtoReadType = this.toNavTypeObject<NisDto>()
}