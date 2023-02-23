package fr.smarquis.qrcode.ui.oneshot

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import fr.smarquis.qrcode.settings.SettingsRepository
import fr.smarquis.qrcode.ui.oneshot.OneShotResult.Found
import fr.smarquis.qrcode.ui.oneshot.OneShotResult.NotFound
import fr.smarquis.qrcode.utils.DecoderDispatcher
import kotlinx.coroutines.flow.first


/*@HiltViewModel not compatible with @AssistedInject: https://github.com/google/dagger/issues/2287 */
class OneShotDecoderViewModel @AssistedInject constructor(
    private val decoder: DecoderDispatcher,
    private val settings: SettingsRepository,
    @Assisted intent: Intent,
) : ViewModel() {

    val result: LiveData<OneShotResult> = liveData {
        when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }.takeIf {
            intent.type?.startsWith("image/") == true
        }?.let {
            decoder.decode(it)
        }.let {
            if (it == null) NotFound
            else Found(it, settings.mode.first())
        }.let {
            emit(it)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(intent: Intent): OneShotDecoderViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: Factory,
            intent: Intent,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = assistedFactory.create(intent) as T
        }
    }

}