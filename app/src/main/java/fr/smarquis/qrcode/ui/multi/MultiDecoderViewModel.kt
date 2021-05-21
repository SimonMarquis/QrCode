package fr.smarquis.qrcode.ui.multi

import android.os.SystemClock
import androidx.annotation.WorkerThread
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.smarquis.qrcode.model.Barcode
import fr.smarquis.qrcode.model.Decoder
import fr.smarquis.qrcode.model.Mode
import fr.smarquis.qrcode.model.Mode.AUTO
import fr.smarquis.qrcode.model.Mode.MANUAL
import fr.smarquis.qrcode.model.Theme
import fr.smarquis.qrcode.settings.SettingsRepository
import fr.smarquis.qrcode.ui.multi.Event.Finish
import fr.smarquis.qrcode.ui.multi.Event.Recreate
import fr.smarquis.qrcode.ui.multi.MultiResult.Empty
import fr.smarquis.qrcode.utils.DecoderDispatcher
import fr.smarquis.qrcode.utils.SingleLiveEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class MultiDecoderViewModel @Inject constructor(
    private val decoder: DecoderDispatcher,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _results = MutableLiveData<MultiResult>()
    val results: LiveData<MultiResult> get() = _results

    private val _events = SingleLiveEvent<Event>()
    val events: LiveData<Event> get() = _events

    //region Settings
    fun decoder(decoder: Decoder) = viewModelScope.launch {
        if (settings.decoder.first() == decoder) return@launch
        settings.decoder(decoder)
        reset()
    }

    fun mode(mode: Mode) = viewModelScope.launch {
        if (settings.mode.first() == mode) return@launch
        settings.mode(mode)
        reset()
    }

    fun theme(theme: Theme) = viewModelScope.launch {
        if (settings.theme.first() == theme) return@launch
        settings.theme(theme)
        _events.value = Recreate
    }
    //endregion

    @WorkerThread
    fun processImage(image: ImageProxy) = runBlocking {
        decoder.decode(image).getOrNull()?.takeIf {
            when (settings.mode.first()) {
                MANUAL -> enoughTimeElapsed(barcode(), 500) && _results.value != it
                AUTO -> enoughTimeElapsed(barcode(), 5000)
            }
        }?.let {
            MultiResult.Found(it, settings.mode.first())
        }?.let {
            _results.postValue(it)
        }
    }

    fun requestShowMore() = viewModelScope.launch {
        _events.value = Event.ShowMore(
            decoder = settings.decoder.first(),
            mode = settings.mode.first(),
            theme = settings.theme.first(),
        )
    }

    fun reset() {
        _results.value = Empty
    }

    fun onBackPressed() = viewModelScope.launch {
        when (settings.mode.first()) {
            MANUAL -> when (barcode()) {
                is Barcode -> reset()
                null -> _events.value = Finish
            }
            AUTO -> _events.value = Finish
        }
    }

    private fun barcode(): Barcode? = when (val it = results.value) {
        is MultiResult.Found -> it.barcode
        Empty, null -> null
    }

    private fun enoughTimeElapsed(old: Barcode?, timeout: Long) = if (old == null) true else SystemClock.elapsedRealtime() - old.timestamp > timeout

}