package fr.smarquis.qrcode.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import fr.smarquis.qrcode.model.Theme
import fr.smarquis.qrcode.settings.ThemeSetting
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

open class DecoderActivity : AppCompatActivity() {

    lateinit var customTheme: Theme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        customTheme = runBlocking {
            ThemeSetting.get(this@DecoderActivity).first()
        }.also {
            AppCompatDelegate.setDefaultNightMode(it.value)
        }
    }

}
