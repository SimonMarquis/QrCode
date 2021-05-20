package fr.smarquis.qrcode.single

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import fr.smarquis.qrcode.model.Barcode
import fr.smarquis.qrcode.model.DecoderHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/*@HiltViewModel not compatible with @AssistedInject: https://github.com/google/dagger/issues/2287 */
class SingleDecoderViewModel @AssistedInject constructor(
    private val decoder: DecoderHolder,
    @Assisted intent: Intent,
) : ViewModel() {

    val barcode: LiveData<Barcode?> = liveData {
        when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }.takeIf {
            intent.type?.startsWith("image/") == true
        }?.let {
            decode(it)
        }.let {
            emit(it)
        }
    }

    private suspend fun decode(stream: Uri): Barcode? = withContext(Dispatchers.Default) {
        kotlin.runCatching {
            decoder.decode(stream)
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    @AssistedFactory
    interface Factory {
        fun create(intent: Intent): SingleDecoderViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: Factory,
            intent: Intent,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T = assistedFactory.create(intent) as T
        }
    }

}