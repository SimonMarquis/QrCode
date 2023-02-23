package fr.smarquis.qrcode.utils

import android.app.Application
import android.view.Menu
import androidx.appcompat.widget.PopupMenu
import androidx.core.provider.FontRequest
import androidx.core.view.iterator
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.EmojiCompat.get
import androidx.emoji.text.FontRequestEmojiCompatConfig
import fr.smarquis.qrcode.R
import java.lang.ref.WeakReference

fun Application.initEmojiCompat(): EmojiCompat =
    FontRequest(
        "com.google.android.gms.fonts",
        "com.google.android.gms",
        "Noto Color Emoji Compat",
        R.array.com_google_android_gms_fonts_certs,
    ).let {
        FontRequestEmojiCompatConfig(this, it).setReplaceAll(true)
    }.let(EmojiCompat::init)

fun PopupMenu.emojify() = apply {
    get().registerInitCallback(MenuInitCallback(menu))
}

fun Menu.process(): Unit = iterator().forEach { item ->
    item.title = get().process(item.title ?: "")
    item.subMenu?.process()
}

private class MenuInitCallback(menu: Menu) : EmojiCompat.InitCallback() {
    private val menu = WeakReference(menu)
    override fun onInitialized() = menu.get()?.process() ?: Unit
}
