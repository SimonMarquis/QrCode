package fr.smarquis.qrcode.settings

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.smarquis.qrcode.model.Theme
import fr.smarquis.qrcode.model.Theme.SYSTEM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object ThemeSetting : Settings<String, Theme>(stringPreferencesKey("theme")) {
    override fun Flow<String?>.mapOut(context: Context): Flow<Theme> = map {
        kotlin.runCatching { Theme.valueOf(requireNotNull(it)) }.getOrDefault(SYSTEM)
    }

    override fun Theme.mapIn(context: Context): String = name
}