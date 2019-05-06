package fr.smarquis.qrcode

import android.Manifest.permission.CAMERA
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.SoundEffectConstants
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import fr.smarquis.qrcode.Mode.AUTO
import fr.smarquis.qrcode.Mode.MANUAL
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.logcat
import io.fotoapparat.preview.Frame
import io.fotoapparat.selector.back
import io.fotoapparat.view.CameraView
import io.fotoapparat.view.FocusView
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_CAMERA_PERMISSION = 1234
        private const val REQUEST_CODE_PERMISSION_SETTINGS = 4321
        private val CUSTOM_TABS_INTENT = CustomTabsIntent.Builder().setToolbarColor(Color.BLACK).build()
    }

    private val mode: ModeHolder by lazy { ModeHolder.instance(application) }
    private val decoder: DecoderHolder by lazy { DecoderHolder.instance(application) }
    private val barcode: BarcodeHolder by lazy { BarcodeHolder.instance(application) }

    private lateinit var camera: Fotoapparat
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<out View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUi()
    }

    private fun initUi() {
        setContentView(R.layout.activity_main)
        progressBar.indeterminateDrawable?.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)

        bottomSheetBehavior = from(bottomSheetLinearLayout)
        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == STATE_COLLAPSED) {
                    barcode.update(null, mode.get())
                    render()
                }
            }
        })

        val applySettingsState = { popup: PopupMenu ->
            popup.menu.apply {
                findItem(R.id.menu_item_scanner_mlkit).isEnabled = Decoder.MLKit.isAvailable
                when (decoder.get()) {
                    Decoder.MLKit -> findItem(R.id.menu_item_scanner_mlkit).isChecked = true
                    Decoder.ZXing -> findItem(R.id.menu_item_scanner_zxing).isChecked = true
                }
                when (mode.get()) {
                    AUTO -> findItem(R.id.menu_item_mode_auto).isChecked = true
                    MANUAL -> findItem(R.id.menu_item_mode_manual).isChecked = true
                }
            }
        }
        val settings = PopupMenu(ContextThemeWrapper(this, R.style.PopupMenu), moreImageView, Gravity.END)
        settings.inflate(R.menu.menu_main)
        settings.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_item_scanner_mlkit -> decoder.set(Decoder.MLKit)
                R.id.menu_item_scanner_zxing -> decoder.set(Decoder.ZXing)
                R.id.menu_item_mode_auto -> mode.set(AUTO)
                R.id.menu_item_mode_manual -> mode.set(MANUAL)
                R.id.menu_item_generator -> CUSTOM_TABS_INTENT.launchUrl(this, getString(R.string.webapp).toUri()).run { true }
                else -> true
            }.also { applySettingsState(settings) }
        }
        moreImageView.setOnClickListener {
            applySettingsState(settings)
            settings.show()
        }

        openImageView.setOnClickListener { safeStartIntent(this, barcode.get()?.intent) }
        copyImageView.setOnClickListener { copyToClipboard(this, barcode.get()?.value) }

        headerLinearLayout.setOnClickListener {
            barcode.get() ?: return@setOnClickListener
            bottomSheetBehavior.state = when (bottomSheetBehavior.state) {
                STATE_COLLAPSED -> STATE_EXPANDED
                STATE_EXPANDED -> STATE_COLLAPSED
                else -> return@setOnClickListener
            }
        }

        camera = Fotoapparat(
            context = this,
            view = findViewById<CameraView>(R.id.cameraView),
            focusView = findViewById<FocusView>(R.id.focusView),
            logger = logcat(),
            lensPosition = back(),
            cameraConfiguration = CameraConfiguration(frameProcessor = this@MainActivity::processFrame),
            cameraErrorCallback = { Toast.makeText(this, it.message, Toast.LENGTH_LONG).show() }
        )

        render()
    }

    @WorkerThread
    private fun processFrame(frame: Frame) {
        try {
            val barcode = decoder.process(this, frame) ?: return
            runOnUiThread { onBarcodeFound(barcode) }
        } catch (e: Exception) {
            Log.e(TAG, "processFrame()", e)
            runOnUiThread { reset() }
        }
    }

    @UiThread
    private fun reset(): Boolean {
        return when (bottomSheetBehavior.state) {
            STATE_DRAGGING, STATE_SETTLING, STATE_EXPANDED, STATE_HALF_EXPANDED -> {
                bottomSheetBehavior.state = STATE_COLLAPSED
                true
            }
            else /*STATE_COLLAPSED, STATE_HIDDEN*/ -> {
                barcode.update(null, mode.get()).also {
                    render()
                }
            }
        }
    }

    @UiThread
    private fun onBarcodeFound(found: Barcode) {
        if (!barcode.update(found, mode.get())) return
        coordinatorLayout.playSoundEffect(SoundEffectConstants.CLICK)
        render()
        if (mode.get() == AUTO && lifecycle.currentState.isAtLeast(RESUMED)) {
            if (!safeStartIntent(this, found.intent)) {
                copyToClipboard(this, found.value)
            }
        }
    }

    @UiThread
    private fun render(render: Barcode? = barcode.get()) {
        when {
            !hasCameraPermission() -> {
                bottomSheetBehavior.isHideable = true
                bottomSheetBehavior.state = STATE_HIDDEN
            }
            render == null || mode.get() == AUTO -> {
                progressBar.visibility = VISIBLE
                iconImageView.visibility = GONE
                iconImageView.setImageResource(0)
                titleTextView.setText(R.string.status_scanning)
                openImageView.visibility = GONE
                copyImageView.visibility = GONE
                detailsTextView.visibility = GONE
                rawTextView.visibility = GONE
                bottomSheetBehavior.state = STATE_COLLAPSED
                bottomSheetBehavior.isHideable = false
            }
            else -> {
                if (bottomSheetBehavior.state == STATE_EXPANDED) TransitionManager.beginDelayedTransition(
                    bottomSheetLinearLayout
                )
                progressBar.visibility = GONE
                iconImageView.setImageResource(render.icon)
                iconImageView.visibility = VISIBLE
                titleTextView.text = render.title
                openImageView.visibility = if (render.intent != null) VISIBLE else GONE
                copyImageView.visibility = VISIBLE
                render.details.let {
                    detailsTextView.visibility = if (it.isNullOrBlank()) GONE else VISIBLE
                    detailsTextView.text = it
                }
                render.value.let {
                    rawTextView.visibility = if (it.isBlank()) GONE else VISIBLE
                    rawTextView.text = it
                }
                bottomSheetBehavior.state = STATE_EXPANDED
                bottomSheetBehavior.isHideable = false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (hasCameraPermission()) {
            camera.start()
        } else {
            requestCameraPermission()
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (mode.get() == AUTO) {
            barcode.update(null, mode.get())
        }
    }

    override fun onStop() {
        super.onStop()
        if (hasCameraPermission()) {
            camera.stop()
        }
    }

    private fun hasCameraPermission(): Boolean = checkSelfPermission(this, CAMERA) == PERMISSION_GRANTED

    private fun requestCameraPermission() = requestPermissions(this, arrayOf(CAMERA), REQUEST_CODE_CAMERA_PERMISSION)

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CODE_CAMERA_PERMISSION) return
        if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
            render()
            camera.start()
        } else {
            if (shouldShowRequestPermissionRationale(this, CAMERA)) {
                requestCameraPermission()
            } else {
                val uri = Uri.fromParts("package", packageName, null)
                val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS, uri)
                startActivityForResult(intent, REQUEST_CODE_PERMISSION_SETTINGS)
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE_PERMISSION_SETTINGS) return
        if (!hasCameraPermission()) {
            requestCameraPermission()
        }
    }

    override fun onBackPressed() {
        if (reset()) return
        super.onBackPressed()
    }

}
