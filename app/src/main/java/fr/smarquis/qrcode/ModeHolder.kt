package fr.smarquis.qrcode

import android.app.Application
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import java.util.concurrent.atomic.AtomicReference

class ModeHolder private constructor(application: Application) {

    companion object : Singleton<ModeHolder, Application>(::ModeHolder) {
        private const val SHARED_PREFERENCES_KEY = "mode"
    }

    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    private val reference: AtomicReference<Mode>

    init {
        val hasTouchScreen = application.packageManager.hasSystemFeature("android.hardware.touchscreen").also {
            Log.d(TAG, "hasSystemFeature(\"android.hardware.touchscreen\") -> $it")
        }
        val forceAuto = when {
            Build.PRODUCT == "m300" || Build.MODEL == "M300" || Build.DEVICE == "vm300" -> true
            else -> false
        }
        val extracted = try {
            Mode.valueOf(sharedPreferences.getString(SHARED_PREFERENCES_KEY, null).orEmpty())
        } catch (e: Exception) {
            null
        }
        reference = AtomicReference(
            when {
                extracted != null -> extracted
                forceAuto -> Mode.AUTO
                hasTouchScreen -> Mode.MANUAL
                else -> Mode.AUTO
            }
        )
    }

    fun get(): Mode = reference.get()

    fun set(mode: Mode): Boolean {
        sharedPreferences.edit().putString(SHARED_PREFERENCES_KEY, reference.get().name).apply()
        return reference.getAndSet(mode) != mode
    }

}