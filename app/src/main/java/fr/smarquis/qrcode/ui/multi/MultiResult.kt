package fr.smarquis.qrcode.ui.multi

import fr.smarquis.qrcode.model.Barcode
import fr.smarquis.qrcode.model.Mode

sealed class MultiResult {
    object Empty : MultiResult()
    data class Found(val barcode: Barcode, val mode: Mode) : MultiResult()
}
