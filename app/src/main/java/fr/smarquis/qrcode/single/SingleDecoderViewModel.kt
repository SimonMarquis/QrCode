package fr.smarquis.qrcode.single

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.*
import fr.smarquis.qrcode.model.Barcode
import fr.smarquis.qrcode.model.DecoderHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SingleDecoderViewModel(application: Application, intent: Intent) : AndroidViewModel(application) {

    private val decoder: DecoderHolder = DecoderHolder.instance(application)

    val barcode: LiveData<Barcode?> = liveData {
        val stream = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM)
        when {
            intent.action != Intent.ACTION_SEND -> emit(null)
            intent.type?.startsWith("image/") != true -> emit(null)
            stream !is Uri -> emit(null)
            else -> emit(decode(stream))
        }
    }

    private suspend fun decode(stream: Uri): Barcode? = withContext(Dispatchers.Default) {
        try {
            decoder.decode(stream)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } as? Barcode

    class Factory(activity: SingleDecoderActivity) : ViewModelProvider.Factory {
        private val application = activity.application
        private val intent = activity.intent
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SingleDecoderViewModel(application, intent) as T
        }
    }

}