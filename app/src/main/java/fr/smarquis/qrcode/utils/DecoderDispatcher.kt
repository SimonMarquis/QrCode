package fr.smarquis.qrcode.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageProxy
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.smarquis.qrcode.di.DefaultDispatcher
import fr.smarquis.qrcode.model.Barcode
import fr.smarquis.qrcode.model.Decoder.MLKit
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

    suspend fun decode(any: Any): Barcode? = decodeInternal(any).getOrNull()

    // Directly exposing Result<Barcode?> fails the tests:
    // `class kotlin.Result cannot be cast to class Barcode`.
    // see: https://youtrack.jetbrains.com/issue/KT-45259
    @VisibleForTesting
    suspend fun decodeInternal(any: Any): Result<Barcode?> = with(settings.decoder.first()) {
        kotlin.runCatching {
            withContext(dispatcher) {
                when (any) {
                    is ImageProxy -> decode(appContext, any)
                    is Uri -> decode(appContext, any)
                    else -> throw UnsupportedOperationException()
                }
            }
        }.onFailure {
            Log.e("Decoder", "Failed to decode $any", it)
        }.recoverCatching {
            if (it is UnsupportedOperationException) throw it
            when (this) {
                is MLKit -> {
                    settings.decoder(ZXing)
                    return decodeInternal(any)
                }
                is ZXing -> throw it
            }
        }
    }

}
