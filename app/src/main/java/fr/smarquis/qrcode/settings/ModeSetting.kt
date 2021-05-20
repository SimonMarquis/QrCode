package fr.smarquis.qrcode.settings

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.smarquis.qrcode.model.Mode
import fr.smarquis.qrcode.model.Mode.AUTO
import fr.smarquis.qrcode.utils.TAG
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object ModeSetting : Settings<String, Mode>(stringPreferencesKey("mode")) {
    override fun Flow<String?>.mapOut(context: Context): Flow<Mode> = map {
        val hasTouchScreen = context.packageManager.hasSystemFeature("android.hardware.touchscreen").also {
            Log.d(TAG, "hasSystemFeature(\"android.hardware.touchscreen\") -> $it")
        }
        val forceAuto = when {
            Build.PRODUCT == "m300" || Build.MODEL == "M300" || Build.DEVICE == "vm300" -> true
            else -> false
        }
        val extracted = kotlin.runCatching {
            Mode.valueOf(it.orEmpty())
        }.getOrNull()
        when {
            extracted != null -> extracted
            forceAuto -> AUTO
            hasTouchScreen -> Mode.MANUAL
            else -> AUTO
        }
    }

    override fun Mode.mapIn(context: Context): String = name
}