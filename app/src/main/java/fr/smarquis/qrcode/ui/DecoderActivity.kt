package fr.smarquis.qrcode.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import fr.smarquis.qrcode.settings.ThemeHolder
import javax.inject.Inject

open class DecoderActivity: AppCompatActivity() {

    @Inject
    protected lateinit var nightMode: ThemeHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(nightMode.get().value)
    }

}