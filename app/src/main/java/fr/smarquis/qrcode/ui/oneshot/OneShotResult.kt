package fr.smarquis.qrcode.ui.oneshot

import fr.smarquis.qrcode.model.Barcode
import fr.smarquis.qrcode.model.Mode

sealed class OneShotResult {
    object NotFound : OneShotResult()
    data class Found(val barcode: Barcode, val mode: Mode) : OneShotResult()
}
