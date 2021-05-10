package fr.smarquis.qrcode.single

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import fr.smarquis.qrcode.model.Barcode
import fr.smarquis.qrcode.model.DecoderHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SingleDecoderViewModel(application: Application, intent: Intent) : AndroidViewModel(application) {

    private val decoder: DecoderHolder = DecoderHolder.instance(application)

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

    class Factory(activity: SingleDecoderActivity) : ViewModelProvider.Factory {
        private val application = activity.application
        private val intent = activity.intent
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SingleDecoderViewModel(application, intent) as T
        }
    }

}