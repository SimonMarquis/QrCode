package fr.smarquis.qrcode

import dagger.hilt.android.HiltAndroidApp
import fr.smarquis.qrcode.utils.checkGooglePlayServices


@HiltAndroidApp
class Application : android.app.Application() {

    override fun onCreate() {
        super.onCreate()
        checkGooglePlayServices(this)
    }

}