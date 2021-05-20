package fr.smarquis.qrcode.model

import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES

enum class Theme(val value: Int) {
    SYSTEM(MODE_NIGHT_FOLLOW_SYSTEM),
    DARK(MODE_NIGHT_YES),
    LIGHT(MODE_NIGHT_NO),
}
