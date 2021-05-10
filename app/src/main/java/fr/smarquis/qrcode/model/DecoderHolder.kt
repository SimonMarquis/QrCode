package fr.smarquis.qrcode.model

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.camera.core.ImageProxy
import androidx.preference.PreferenceManager
import fr.smarquis.qrcode.model.Decoder.MLKit
import fr.smarquis.qrcode.model.Decoder.ZXing
import fr.smarquis.qrcode.utils.*
import java.util.concurrent.atomic.AtomicReference

class DecoderHolder private constructor(private val application: Application) {

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
    fun decode(imageProxy: ImageProxy): Barcode? {
        return process(imageProxy)
    }

    @Throws(java.lang.Exception::class)
    @WorkerThread
    fun decode(uri: Uri): Barcode? {
        return process(uri)
    }

    @Throws(java.lang.Exception::class)
    @WorkerThread
    private fun process(any: Any): Barcode? {
        reference.get().let {
            try {
                return when (any) {
                    is ImageProxy -> it.decode(application, any)
                    is Uri -> it.decode(application, any)
                    else -> throw UnsupportedOperationException()
                }
            } catch (e: Exception) {
                if (it is MLKit) {
                    it.isAvailable = false
                    set(ZXing)
                    return process(any)
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
        Log.d(TAG, "Decoder: `${decoder.name()}`")
    }

}