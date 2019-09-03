package fr.smarquis.qrcode

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.preference.PreferenceManager
import fr.smarquis.qrcode.Decoder.MLKit
import fr.smarquis.qrcode.Decoder.ZXing
import io.fotoapparat.preview.Frame
import java.util.concurrent.atomic.AtomicReference

class DecoderHolder private constructor(application: Application) {

    companion object : Singleton<DecoderHolder, Application>(::DecoderHolder) {
        private const val SHARED_PREFERENCES_KEY = "decoder"
    }

    private val reference: AtomicReference<Decoder>

    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    init {
        checkGooglePlayServices(application)
        MLKit.isAvailable = isGooglePlayServicesAvailable(application)
        val nameToRestore = sharedPreferences.getString(SHARED_PREFERENCES_KEY, null)
        reference = AtomicReference(
            when {
                nameToRestore == MLKit.name() && MLKit.isAvailable -> MLKit
                nameToRestore == ZXing.name() -> ZXing
                MLKit.isAvailable -> MLKit
                else -> ZXing
            }
        )
        log(reference.get())
    }

    @Throws(java.lang.Exception::class)
    @WorkerThread
    fun process(context: Context, frame: Frame): Barcode? {
        reference.get().let {
            try {
                return it.decode(context, frame)
            } catch (e: Exception) {
                if (it is MLKit) {
                    it.isAvailable = false
                    set(ZXing)
                }
                throw e
            }
        }
    }

    fun get(): Decoder = reference.get()

    fun set(decoder: Decoder): Boolean {
        if (decoder is MLKit && !decoder.isAvailable) {
            return false
        }
        log(decoder)
        sharedPreferences.edit().putString(SHARED_PREFERENCES_KEY, decoder.name()).apply()
        return reference.getAndSet(decoder) != decoder
    }

    private fun log(decoder: Decoder) {
        Log.d(TAG, "Process frames with `${decoder.name()}`")
    }

}