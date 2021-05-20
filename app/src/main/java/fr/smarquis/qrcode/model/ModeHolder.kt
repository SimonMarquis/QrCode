package fr.smarquis.qrcode.model

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.smarquis.qrcode.utils.TAG
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModeHolder @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sharedPreferences: SharedPreferences,
) {

    companion object {
        private const val SHARED_PREFERENCES_KEY = "mode"
    }

    private val reference: AtomicReference<Mode>

    init {
        val hasTouchScreen = appContext.packageManager.hasSystemFeature("android.hardware.touchscreen").also {
            Log.d(TAG, "hasSystemFeature(\"android.hardware.touchscreen\") -> $it")
        }
        val forceAuto = when {
            Build.PRODUCT == "m300" || Build.MODEL == "M300" || Build.DEVICE == "vm300" -> true
            else -> false
        }
        val extracted = kotlin.runCatching {
            Mode.valueOf(sharedPreferences.getString(SHARED_PREFERENCES_KEY, null).orEmpty())
        }.getOrNull()
        reference = AtomicReference(
            when {
                extracted != null -> extracted
                forceAuto -> Mode.AUTO
                hasTouchScreen -> Mode.MANUAL
                else -> Mode.AUTO
            }
        )
        log(reference.get())
    }

    fun get(): Mode = reference.get()

    fun set(mode: Mode): Boolean {
        log(mode)
        sharedPreferences.edit().putString(SHARED_PREFERENCES_KEY, mode.name).apply()
        return reference.getAndSet(mode) != mode
    }

    private fun log(mode: Mode) {
        Log.d(TAG, "Mode: `${mode}`")
    }

}