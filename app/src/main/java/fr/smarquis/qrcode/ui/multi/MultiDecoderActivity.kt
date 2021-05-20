package fr.smarquis.qrcode.ui.multi

import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color.BLACK
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.launch
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.camera.core.*
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat.*
import androidx.core.net.toUri
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.RESUMED
import dagger.hilt.android.AndroidEntryPoint
import fr.smarquis.qrcode.R
import fr.smarquis.qrcode.ui.DecoderActivity
import fr.smarquis.qrcode.databinding.ActivityMultiDecoderBinding
import fr.smarquis.qrcode.model.Decoder
import fr.smarquis.qrcode.model.Mode.AUTO
import fr.smarquis.qrcode.model.Mode.MANUAL
import fr.smarquis.qrcode.model.Theme.DARK
import fr.smarquis.qrcode.model.Theme.LIGHT
import fr.smarquis.qrcode.model.Theme.SYSTEM
import fr.smarquis.qrcode.utils.TAG
import fr.smarquis.qrcode.utils.copyToClipboard
import fr.smarquis.qrcode.utils.safeStartIntent
import java.util.concurrent.Executors

@AndroidEntryPoint
class MultiDecoderActivity : DecoderActivity(), PopupMenu.OnMenuItemClickListener {

    companion object {
        private val CUSTOM_TABS_INTENT = CustomTabsIntent.Builder().setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(BLACK).build()).build()
    }

    private lateinit var binding: ActivityMultiDecoderBinding

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var camera: Camera? = null

    private var toast: Toast? = null

    private val viewModel by viewModels<MultiDecoderViewModel>()

    private val settings by lazy {
        PopupMenu(this, binding.barcodeView.anchor(), Gravity.END).apply {
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

    private val requestPermission: ActivityResultLauncher<String> = registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
        when {
            isGranted -> viewModel.reset()
            shouldShowRequestPermissionRationale(this, CAMERA) -> requestCameraPermission()
            else -> openAppDetailsSettings.launch()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUi()
        initViewModel()
        initTouchGestures()
        initCameraProvider()
    }

    private fun initUi() {
        setContentView(ActivityMultiDecoderBinding.inflate(layoutInflater).also { binding = it }.root)
        binding.barcodeView.configure(
            onCollapsed = { viewModel.reset() },
            open = { safeStartIntent(this, it.intent) },
            copy = { toast = copyToClipboard(this, it.value, toast) },
            more = {
                applySettingsState(settings)
                settings.show()
            })
    }

    private fun initViewModel() {
        viewModel.barcode().observe(this) {
            if (it == null) {
                binding.barcodeView.barcode = null
                return@observe
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
        }
    }

    private fun initTouchGestures() {
        val scaleDetector = ScaleGestureDetector(this, object : SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean = camera?.apply {
                cameraControl.setZoomRatio((cameraInfo.zoomState.value?.zoomRatio ?: 0F) * detector.scaleFactor)
            }.let { true }
        })
        val meteringPointFactory = binding.preview.meteringPointFactory
        val gestureDetector = GestureDetectorCompat(this, object : SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = camera?.cameraControl?.startFocusAndMetering(
                FocusMeteringAction.Builder(meteringPointFactory.createPoint(e.x, e.y)).build()
            ).let { super.onDown(e) }
        })
        binding.preview.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            scaleDetector.onTouchEvent(motionEvent)
            gestureDetector.onTouchEvent(motionEvent)
            if (motionEvent.action == MotionEvent.ACTION_DOWN) view.performClick()
            return@setOnTouchListener true
        }
    }

    private fun ProcessCameraProvider.bind(): Camera {
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(binding.preview.surfaceProvider)
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(cameraExecutor, { it.use(viewModel::processImage) })
        unbindAll()
        return bindToLifecycle(this@MultiDecoderActivity, cameraSelector(), preview, imageAnalysis)
    }

    private fun ProcessCameraProvider.cameraSelector() = listOf(DEFAULT_BACK_CAMERA, DEFAULT_FRONT_CAMERA).firstOrNull(::hasCamera) ?: CameraSelector.Builder().build()

    private fun initCameraProvider() {
        val future = ProcessCameraProvider.getInstance(this)
        val listener = Runnable {
            kotlin.runCatching {
                future.get().takeIf { lifecycle.currentState.isAtLeast(CREATED) }?.bind()
            }.onSuccess {
                camera = it
            }.onFailure {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).apply { setGravity(Gravity.CENTER, 0, 0) }.show()
                finish()
            }
        }
        future.addListener(listener, getMainExecutor(this))
    }

    override fun onStart() {
        super.onStart()
        if (!hasCameraPermission()) requestCameraPermission()
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
            when (nightMode.get()) {
                SYSTEM -> findItem(R.id.menu_item_theme_system).isChecked = true
                DARK -> findItem(R.id.menu_item_theme_dark).isChecked = true
                LIGHT -> findItem(R.id.menu_item_theme_light).isChecked = true
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean =
        when (item?.itemId) {
            R.id.menu_item_scanner_mlkit -> viewModel.decoder.set(Decoder.MLKit)
            R.id.menu_item_scanner_zxing -> viewModel.decoder.set(Decoder.ZXing)
            R.id.menu_item_mode_auto -> viewModel.mode.set(AUTO).also { viewModel.reset() }
            R.id.menu_item_mode_manual -> viewModel.mode.set(MANUAL).also { viewModel.reset() }
            R.id.menu_item_theme_system -> nightMode.set(SYSTEM).also { recreate() }
            R.id.menu_item_theme_dark -> nightMode.set(DARK).also { recreate() }
            R.id.menu_item_theme_light -> nightMode.set(LIGHT).also { recreate() }
            R.id.menu_item_generator -> CUSTOM_TABS_INTENT.launchUrl(this, getString(R.string.webapp).toUri()).let { true }
            else -> true
        }.also { applySettingsState(settings) }

    override fun onBackPressed() {
        if (viewModel.reset()) return
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        camera = null
    }

}
