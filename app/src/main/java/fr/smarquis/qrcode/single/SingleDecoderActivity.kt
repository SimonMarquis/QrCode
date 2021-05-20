package fr.smarquis.qrcode.single

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import fr.smarquis.qrcode.R
import fr.smarquis.qrcode.databinding.ActivitySingleDecoderBinding
import fr.smarquis.qrcode.model.Barcode
import fr.smarquis.qrcode.model.Mode.AUTO
import fr.smarquis.qrcode.model.Mode.MANUAL
import fr.smarquis.qrcode.model.ModeHolder
import fr.smarquis.qrcode.utils.copyToClipboard
import fr.smarquis.qrcode.utils.safeStartIntent
import javax.inject.Inject

@AndroidEntryPoint
class SingleDecoderActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySingleDecoderBinding

    @Inject lateinit var mode: ModeHolder

    @Inject lateinit var factory: SingleDecoderViewModel.Factory

    private val viewModel: SingleDecoderViewModel by viewModels { SingleDecoderViewModel.provideFactory(factory, intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivitySingleDecoderBinding.inflate(layoutInflater).also { binding = it }.root)
        binding.background.setOnClickListener { finish() }
        binding.barcodeView.configure(
            onCollapsed = { finish() },
            open = { safeStartIntent(this, it.intent) },
            copy = { copyToClipboard(this, it.value) }
        )
        viewModel.barcode.observe(this, ::onBarcode)
    }

    private fun onBarcode(it: Barcode?) {
        binding.barcodeView.barcode = it
        if (it == null) {
            Toast.makeText(this, R.string.toast_decoder_did_not_find_anything, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        when (mode.get()) {
            AUTO -> {
                if (!safeStartIntent(this, it.intent)) {
                    copyToClipboard(this, it.value)
                }
                finish()
            }
            MANUAL -> Unit
        }
    }

}
