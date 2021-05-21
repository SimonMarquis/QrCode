package fr.smarquis.qrcode.settings

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.smarquis.qrcode.model.Decoder
import fr.smarquis.qrcode.utils.isGooglePlayServicesAvailable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object DecoderSetting : Settings<String, Decoder>(stringPreferencesKey("decoder")) {
    override fun Flow<String?>.mapOut(context: Context): Flow<Decoder> = map {
        Decoder.MLKit.isAvailable = isGooglePlayServicesAvailable(context)
        when {
            it == Decoder.MLKit.name() && Decoder.MLKit.isAvailable -> Decoder.MLKit
            it == Decoder.ZXing.name() -> Decoder.ZXing
            Decoder.MLKit.isAvailable -> Decoder.MLKit
            else -> Decoder.ZXing
        }
    }

    override fun Decoder.mapIn(context: Context): String = name()
}