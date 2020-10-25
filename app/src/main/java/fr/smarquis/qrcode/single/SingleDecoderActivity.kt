package fr.smarquis.qrcode.single

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import fr.smarquis.qrcode.R
import fr.smarquis.qrcode.model.Mode.AUTO
import fr.smarquis.qrcode.model.Mode.MANUAL
import fr.smarquis.qrcode.model.ModeHolder
import fr.smarquis.qrcode.utils.copyToClipboard
import fr.smarquis.qrcode.utils.safeStartIntent
import kotlinx.android.synthetic.main.activity_multi_decoder.barcodeView
import kotlinx.android.synthetic.main.activity_single_decoder.*


class SingleDecoderActivity : AppCompatActivity() {

    private val viewModel by viewModels<SingleDecoderViewModel> { SingleDecoderViewModel.Factory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_decoder)
        background.setOnClickListener { finish() }
        barcodeView.configure(
            onCollapsed = { finish() },
            open = { safeStartIntent(this, it.intent) },
            copy = { copyToClipboard(this, it.value) }
        )
        viewModel.barcode.observe(this, {
            barcodeView.barcode = it
            val mode = ModeHolder.instance(application).get()
            when {
                it == null -> {
                    Toast.makeText(this, R.string.toast_decoder_did_not_find_anything, Toast.LENGTH_LONG).show()
                    finish()
                }
                mode == AUTO -> {
                    if (!safeStartIntent(this, it.intent)) {
                        copyToClipboard(this, it.value)
                    }
                    finish()
                }
                mode == MANUAL -> Unit
            }
        })
    }

}
