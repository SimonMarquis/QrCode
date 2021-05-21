package fr.smarquis.qrcode.utils

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageProxy
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.smarquis.qrcode.di.DefaultDispatcher
import fr.smarquis.qrcode.model.Barcode
import fr.smarquis.qrcode.model.Decoder.MLKit
import fr.smarquis.qrcode.model.Decoder.MLKit.isAvailable
import fr.smarquis.qrcode.model.Decoder.ZXing
import fr.smarquis.qrcode.settings.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DecoderDispatcher @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    private val settings: SettingsRepository,
) {

    suspend fun decode(any: Any): Result<Barcode?> = with(settings.decoder.first()) {
        kotlin.runCatching {
            withContext(dispatcher) {
                when (any) {
                    is ImageProxy -> decode(appContext, any)
                    is Uri -> decode(appContext, any)
                    else -> throw UnsupportedOperationException()
                }
            }
        }.onFailure {
            it.printStackTrace()
        }.recoverCatching {
            when (this) {
                MLKit -> {
                    isAvailable = false
                    settings.decoder(ZXing)
                    return decode(any)
                }
                ZXing -> throw it
            }
        }
    }

}
