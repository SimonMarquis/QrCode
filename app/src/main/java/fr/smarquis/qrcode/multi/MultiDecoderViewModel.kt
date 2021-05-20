package fr.smarquis.qrcode.multi

import android.os.SystemClock
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.smarquis.qrcode.model.Barcode
import fr.smarquis.qrcode.settings.DecoderHolder
import fr.smarquis.qrcode.model.Mode.AUTO
import fr.smarquis.qrcode.model.Mode.MANUAL
import fr.smarquis.qrcode.settings.ModeHolder
import fr.smarquis.qrcode.settings.ThemeHolder
import fr.smarquis.qrcode.utils.TAG
import javax.inject.Inject

@HiltViewModel
class MultiDecoderViewModel @Inject constructor(
    val decoder: DecoderHolder,
    val mode: ModeHolder,
) : ViewModel() {

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