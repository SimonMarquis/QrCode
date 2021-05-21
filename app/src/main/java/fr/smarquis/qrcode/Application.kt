package fr.smarquis.qrcode

import dagger.hilt.android.HiltAndroidApp
import fr.smarquis.qrcode.model.Decoder.MLKit
import fr.smarquis.qrcode.utils.checkGooglePlayServices
import fr.smarquis.qrcode.utils.isGooglePlayServicesAvailable


@HiltAndroidApp
class Application : android.app.Application() {

    override fun onCreate() {
        super.onCreate()
        checkGooglePlayServices(this)
        MLKit.isAvailable = isGooglePlayServicesAvailable(this)
    }

}