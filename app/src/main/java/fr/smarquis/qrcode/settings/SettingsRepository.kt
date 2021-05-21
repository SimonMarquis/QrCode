package fr.smarquis.qrcode.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.smarquis.qrcode.model.Decoder
import fr.smarquis.qrcode.model.Mode
import fr.smarquis.qrcode.model.Theme
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext val appContext: Context) {

    val mode: Flow<Mode> = ModeSetting.get(appContext)
    suspend fun mode(mode: Mode) = ModeSetting.set(appContext, mode)

    val theme: Flow<Theme> = ThemeSetting.get(appContext)
    suspend fun theme(theme: Theme) = ThemeSetting.set(appContext, theme)

    val decoder: Flow<Decoder> = DecoderSetting.get(appContext)
    suspend fun decoder(decoder: Decoder) = DecoderSetting.set(appContext, decoder)

}