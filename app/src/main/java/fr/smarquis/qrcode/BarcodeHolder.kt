package fr.smarquis.qrcode

import android.app.Application
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicReference

class BarcodeHolder private constructor(@Suppress("unused") val application: Application) {

    companion object : Singleton<BarcodeHolder, Application>(::BarcodeHolder)

    private val reference: AtomicReference<Barcode?> = AtomicReference()

    fun get(): Barcode? = reference.get()

    fun update(new: Barcode?, mode: Mode): Boolean {
        if (new == null) {
            reference.set(null)
            return reference.getAndSet(null) != null
        }
        val old = reference.get()
        val barcode = when {
            old == null -> new
            mode == Mode.MANUAL && enoughTimeElapsed(old, 500) && isDifferent(old, new) -> new
            mode == Mode.AUTO && enoughTimeElapsed(old, 5000) -> new
            else -> return false
        }
        reference.set(barcode)
        return true
    }

    private fun enoughTimeElapsed(old: Barcode, timeout: Long) = SystemClock.elapsedRealtime() - old.timestamp > timeout

    private fun isDifferent(l: Barcode, r: Barcode) = l::class != r::class || l.format != r.format || l.value != r.value

}