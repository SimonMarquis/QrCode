package fr.smarquis.qrcode.settings

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.smarquis.qrcode.model.Decoder
import fr.smarquis.qrcode.model.Decoder.MLKit
import fr.smarquis.qrcode.model.Decoder.ZXing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object DecoderSetting : Settings<String, Decoder>(stringPreferencesKey("decoder")) {
    override fun Flow<String?>.mapOut(context: Context): Flow<Decoder> = map {
        when {
            it == MLKit.name() && MLKit.isAvailable -> MLKit
            it == ZXing.name() -> ZXing
            else -> if (MLKit.isAvailable) MLKit else ZXing
        }
    }

    override fun Decoder.mapIn(context: Context): String = name()
}