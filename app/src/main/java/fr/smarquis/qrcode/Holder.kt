package fr.smarquis.qrcode

import android.app.Application
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicReference

class Holder private constructor(@Suppress("unused") val application: Application) {

    companion object : Singleton<Holder, Application>(::Holder)

    private val reference: AtomicReference<Barcode?> = AtomicReference()

    fun get(): Barcode? = reference.get()

    fun update(new: Barcode?, mode: Mode): Boolean {
        val old = reference.get()
        if (new == null) {
            reference.set(null)
            return old != null
        }
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

    private fun isDifferent(old: Barcode, new: Barcode) = new::class != old::class || new.format != old.format || new.value != old.value

}