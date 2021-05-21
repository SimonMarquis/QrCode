package fr.smarquis.qrcode.ui.oneshot

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import fr.smarquis.qrcode.R
import fr.smarquis.qrcode.databinding.ActivitySingleDecoderBinding
import fr.smarquis.qrcode.model.Mode.AUTO
import fr.smarquis.qrcode.model.Mode.MANUAL
import fr.smarquis.qrcode.ui.DecoderActivity
import fr.smarquis.qrcode.ui.oneshot.OneShotResult.Found
import fr.smarquis.qrcode.ui.oneshot.OneShotResult.NotFound
import fr.smarquis.qrcode.utils.copyToClipboard
import fr.smarquis.qrcode.utils.safeStartIntent
import javax.inject.Inject

@AndroidEntryPoint
class OneShotDecoderActivity : DecoderActivity() {

    private lateinit var binding: ActivitySingleDecoderBinding

    @Inject
    lateinit var factory: OneShotDecoderViewModel.Factory

    private val viewModel: OneShotDecoderViewModel by viewModels {
        OneShotDecoderViewModel.provideFactory(factory, intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivitySingleDecoderBinding.inflate(layoutInflater).also { binding = it }.root)
        binding.background.setOnClickListener { finish() }
        binding.barcodeView.configure(
            onCollapsed = { finish() },
            open = { safeStartIntent(this, it.intent) },
            copy = { copyToClipboard(this, it.value) }
        )
        viewModel.result.observe(this, ::onOneShotResult)
    }

    private fun onOneShotResult(result: OneShotResult) = when (result) {
        is Found -> onOneShotResultFound(result)
        NotFound -> onOneShotResultNotFound()
    }

    private fun onOneShotResultFound(found: Found) {
        val (barcode, mode) = found
        binding.barcodeView.barcode = barcode
        when (mode) {
            AUTO -> {
                if (!safeStartIntent(this, barcode.intent)) {
                    copyToClipboard(this, barcode.value)
                }
                finish()
            }
            MANUAL -> Unit
        }
    }

    private fun onOneShotResultNotFound() {
        Toast.makeText(this, R.string.toast_decoder_did_not_find_anything, Toast.LENGTH_LONG).show()
        finish()
    }

}
