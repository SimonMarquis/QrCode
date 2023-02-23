package fr.smarquis.qrcode.ui.multi

import fr.smarquis.qrcode.model.Decoder
import fr.smarquis.qrcode.model.Mode
import fr.smarquis.qrcode.model.Theme

sealed class Event {

    data class ShowMore(val decoder: Decoder, val mode: Mode, val theme: Theme) : Event()
    object Recreate : Event()
    object Finish : Event()

}
