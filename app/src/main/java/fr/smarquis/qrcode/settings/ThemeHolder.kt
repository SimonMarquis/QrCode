package fr.smarquis.qrcode.settings

import android.content.SharedPreferences
import android.util.Log
import fr.smarquis.qrcode.model.Theme
import fr.smarquis.qrcode.utils.TAG
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeHolder @Inject constructor(
    private val sharedPreferences: SharedPreferences,
) {

    companion object {
        private const val SHARED_PREFERENCES_KEY = "theme"
    }

    private val reference: AtomicReference<Theme> = AtomicReference(kotlin.runCatching {
        Theme.valueOf(sharedPreferences.getString(SHARED_PREFERENCES_KEY, null).orEmpty())
    }.getOrDefault(Theme.SYSTEM))

    init {
        log(reference.get())
    }

    fun get(): Theme = reference.get()

    fun set(theme: Theme): Boolean {
        log(theme)
        sharedPreferences.edit().putString(SHARED_PREFERENCES_KEY, theme.name).apply()
        return reference.getAndSet(theme) != theme
    }

    private fun log(theme: Theme) = Log.d(TAG, "Theme: `${theme}`")

}