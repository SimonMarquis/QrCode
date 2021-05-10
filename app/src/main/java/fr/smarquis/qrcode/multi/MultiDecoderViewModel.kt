package fr.smarquis.qrcode.multi

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.smarquis.qrcode.model.Barcode
import fr.smarquis.qrcode.model.DecoderHolder
import fr.smarquis.qrcode.model.Mode.AUTO
import fr.smarquis.qrcode.model.Mode.MANUAL
import fr.smarquis.qrcode.model.ModeHolder
import fr.smarquis.qrcode.utils.TAG

class MultiDecoderViewModel(application: Application) : AndroidViewModel(application) {

    val decoder: DecoderHolder = DecoderHolder.instance(application)

    val mode: ModeHolder = ModeHolder.instance(application)

    private val _barcode: MutableLiveData<Barcode?> = MutableLiveData(null)

    fun barcode(): LiveData<Barcode?> = _barcode

    @WorkerThread
    fun processImage(image: ImageProxy) {
        kotlin.runCatching {
            decoder.decode(image)
        }.onFailure {
            Log.e(TAG, "processImage()", it)
        }.getOrNull()?.takeIf {
            when (mode.get()) {
                MANUAL -> enoughTimeElapsed(_barcode.value, 500) && _barcode.value != it
                AUTO -> enoughTimeElapsed(_barcode.value, 5000)
            }
        }?.let { _barcode.postValue(it) }
    }

    fun reset(): Boolean {
        val reset = _barcode.value != null
        _barcode.postValue(null)
        return when (mode.get()) {
            AUTO -> false
            MANUAL -> reset
        }
    }

    private fun enoughTimeElapsed(old: Barcode?, timeout: Long) = if (old == null) true else SystemClock.elapsedRealtime() - old.timestamp > timeout

}