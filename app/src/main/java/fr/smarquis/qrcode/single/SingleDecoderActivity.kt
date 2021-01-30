package fr.smarquis.qrcode.single

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import fr.smarquis.qrcode.R
import fr.smarquis.qrcode.databinding.ActivitySingleDecoderBinding
import fr.smarquis.qrcode.model.Mode.AUTO
import fr.smarquis.qrcode.model.Mode.MANUAL
import fr.smarquis.qrcode.model.ModeHolder
import fr.smarquis.qrcode.utils.copyToClipboard
import fr.smarquis.qrcode.utils.safeStartIntent


class SingleDecoderActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySingleDecoderBinding

    private val viewModel by viewModels<SingleDecoderViewModel> { SingleDecoderViewModel.Factory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivitySingleDecoderBinding.inflate(layoutInflater).also { binding = it }.root)
        binding.background.setOnClickListener { finish() }
        binding.barcodeView.configure(
            onCollapsed = { finish() },
            open = { safeStartIntent(this, it.intent) },
            copy = { copyToClipboard(this, it.value) }
        )
        viewModel.barcode.observe(this, {
            binding.barcodeView.barcode = it
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
