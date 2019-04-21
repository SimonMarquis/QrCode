package fr.smarquis.qrcode

import android.app.Application
import android.os.Build
import android.util.Log

enum class Mode {
    MANUAL,
    AUTO;

    companion object : Singleton<Mode, Application>({ context ->
        val hasTouchScreen = context.packageManager.hasSystemFeature("android.hardware.touchscreen").also {
            Log.d(TAG, "hasSystemFeature(\"android.hardware.touchscreen\") -> $it")
        }

        val forceAuto = when {
            Build.PRODUCT == "m300" || Build.MODEL == "M300" || Build.DEVICE == "vm300" -> true
            else -> false
        }
        when {
            forceAuto -> AUTO
            hasTouchScreen -> MANUAL
            else -> AUTO
        }
    })

}