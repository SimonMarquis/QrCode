package fr.smarquis.qrcode.multi

import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color.BLACK
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MenuItem
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Observer
import fr.smarquis.qrcode.R
import fr.smarquis.qrcode.databinding.ActivityMultiDecoderBinding
import fr.smarquis.qrcode.model.Decoder
import fr.smarquis.qrcode.model.Mode.AUTO
import fr.smarquis.qrcode.model.Mode.MANUAL
import fr.smarquis.qrcode.utils.TAG
import fr.smarquis.qrcode.utils.copyToClipboard
import fr.smarquis.qrcode.utils.safeStartIntent
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.logcat
import io.fotoapparat.selector.back
import io.fotoapparat.view.CameraView
import io.fotoapparat.view.FocusView


class MultiDecoderActivity : AppCompatActivity(), PopupMenu.OnMenuItemClickListener {

    companion object {
        private val CUSTOM_TABS_INTENT = CustomTabsIntent.Builder().setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(BLACK).build()).build()
    }

    private lateinit var binding: ActivityMultiDecoderBinding

    private lateinit var camera: Fotoapparat

    private var toast: Toast? = null

    private val viewModel by viewModels<MultiDecoderViewModel>()

    private val settings by lazy {
        PopupMenu(ContextThemeWrapper(this, R.style.PopupMenu), binding.barcodeView.anchor(), Gravity.END).apply {
            inflate(R.menu.menu_multi_decoder)
            setOnMenuItemClickListener(this@MultiDecoderActivity)
        }
    }

    private val openAppDetailsSettings: ActivityResultLauncher<Void?> = registerForActivityResult(object : ActivityResultContract<Void?, Boolean>() {
        override fun createIntent(context: Context, input: Void?): Intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        override fun parseResult(resultCode: Int, intent: Intent?): Boolean = hasCameraPermission()
    }) { hasCameraPermission ->
        if (!hasCameraPermission) requestCameraPermission()
    }

    private val requestPermission: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        when {
            isGranted -> {
                viewModel.reset()
                camera.start()
            }
            shouldShowRequestPermissionRationale(this, CAMERA) -> requestCameraPermission()
            else -> openAppDetailsSettings.launch()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityMultiDecoderBinding.inflate(layoutInflater).also { binding = it }.root)
        camera = Fotoapparat(
            context = this,
            view = findViewById<CameraView>(R.id.cameraView),
            focusView = findViewById<FocusView>(R.id.focusView),
            logger = logcat(),
            lensPosition = back(),
            cameraConfiguration = CameraConfiguration(frameProcessor = viewModel::processFrame),
            cameraErrorCallback = {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).apply { setGravity(Gravity.CENTER, 0, 0) }.show()
                finish()
            }
        )
        binding.barcodeView.configure(
            onCollapsed = { viewModel.reset() },
            open = { safeStartIntent(this, it.intent) },
            copy = { toast = copyToClipboard(this, it.value, toast) },
            more = {
                applySettingsState(settings)
                settings.show()
            })
        binding.zoomView.configure(
            zoom = { camera.setZoom(it) },
            focus = binding.focusView
        )

        viewModel.barcode().observe(this, Observer {
            if (it == null) {
                binding.barcodeView.barcode = null
                return@Observer
            }
            Log.d(TAG, "onBarcode($it)")
            binding.coordinatorLayout.playSoundEffect(SoundEffectConstants.CLICK)
            when (viewModel.mode.get()) {
                MANUAL -> binding.barcodeView.barcode = it
                AUTO -> {
                    if (lifecycle.currentState.isAtLeast(RESUMED) && !safeStartIntent(this, it.intent)) {
                        toast = copyToClipboard(this, it.value, toast)
                    }
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (hasCameraPermission()) camera.start()
        else requestCameraPermission()
    }

    override fun onStop() {
        super.onStop()
        if (hasCameraPermission()) camera.stop()
    }

    private fun hasCameraPermission(): Boolean = checkSelfPermission(this, CAMERA) == PERMISSION_GRANTED

    private fun requestCameraPermission() = requestPermission.launch(CAMERA)

    private fun applySettingsState(popup: PopupMenu) {
        popup.menu.apply {
            findItem(R.id.menu_item_scanner_mlkit).isEnabled = Decoder.MLKit.isAvailable
            when (viewModel.decoder.get()) {
                Decoder.MLKit -> findItem(R.id.menu_item_scanner_mlkit).isChecked = true
                Decoder.ZXing -> findItem(R.id.menu_item_scanner_zxing).isChecked = true
            }
            when (viewModel.mode.get()) {
                AUTO -> findItem(R.id.menu_item_mode_auto).isChecked = true
                MANUAL -> findItem(R.id.menu_item_mode_manual).isChecked = true
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean =
        when (item?.itemId) {
            R.id.menu_item_scanner_mlkit -> viewModel.decoder.set(Decoder.MLKit)
            R.id.menu_item_scanner_zxing -> viewModel.decoder.set(Decoder.ZXing)
            R.id.menu_item_mode_auto -> viewModel.mode.set(AUTO).also { viewModel.reset() }
            R.id.menu_item_mode_manual -> viewModel.mode.set(MANUAL).also { viewModel.reset() }
            R.id.menu_item_generator -> CUSTOM_TABS_INTENT.launchUrl(this, getString(R.string.webapp).toUri()).let { true }
            else -> true
        }.also { applySettingsState(settings) }

    override fun onBackPressed() {
        if (viewModel.reset()) return
        super.onBackPressed()
    }

}
