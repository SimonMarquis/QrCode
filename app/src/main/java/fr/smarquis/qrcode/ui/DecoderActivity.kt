package fr.smarquis.qrcode.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import fr.smarquis.qrcode.model.Theme
import fr.smarquis.qrcode.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

open class DecoderActivity : AppCompatActivity() {

    @Inject
    lateinit var settings: SettingsRepository

    lateinit var customTheme: Theme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        customTheme = runBlocking {
            settings.theme.first()
        }.also {
            AppCompatDelegate.setDefaultNightMode(it.value)
        }
    }

}
