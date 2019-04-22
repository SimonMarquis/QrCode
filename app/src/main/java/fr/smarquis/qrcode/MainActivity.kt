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
import android.view.KeyEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import fr.smarquis.qrcode.Mode.AUTO
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
    }

    private val mode: Mode by lazy { Mode.instance(application) }
    private val decoder: Decoder by lazy { Decoder.instance(application) }
    private val holder: Holder by lazy { Holder.instance(application) }

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
                    holder.update(null, mode)
                    render()
                }
            }
        })

        openImageView.setOnClickListener { safeStartIntent(this, holder.get()?.intent) }
        copyImageView.setOnClickListener { copyToClipboard(this, holder.get()?.value) }

        headerLinearLayout.setOnClickListener {
            holder.get() ?: return@setOnClickListener
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
            val barcode = decoder.decode(this, frame) ?: return
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
                holder.update(null, mode).also {
                    render()
                }
            }
        }
    }

    @UiThread
    private fun onBarcodeFound(barcode: Barcode) {
        if (!holder.update(barcode, mode)) return
        coordinatorLayout.playSoundEffect(SoundEffectConstants.CLICK)
        render()
        if (mode == AUTO && lifecycle.currentState.isAtLeast(RESUMED)) {
            if (!safeStartIntent(this, barcode.intent)) {
                copyToClipboard(this, barcode.value)
            }
        }
    }

    @UiThread
    private fun render(barcode: Barcode? = holder.get()) {
        when {
            !hasCameraPermission() -> {
                bottomSheetBehavior.isHideable = true
                bottomSheetBehavior.state = STATE_HIDDEN
            }
            barcode == null || mode == AUTO -> {
                progressBar.visibility = VISIBLE
                iconImageView.visibility = GONE
                iconImageView.setImageResource(0)
                titleTextView.text = getString(R.string.status_scanning_with, decoder.internal())
                openImageView.visibility = GONE
                copyImageView.visibility = GONE
                detailsTextView.visibility = GONE
                rawTextView.visibility = GONE
                bottomSheetBehavior.state = STATE_COLLAPSED
                bottomSheetBehavior.isHideable = false
            }
            else -> {
                if (bottomSheetBehavior.state == STATE_EXPANDED) TransitionManager.beginDelayedTransition(bottomSheetLinearLayout)
                progressBar.visibility = GONE
                iconImageView.setImageResource(barcode.icon)
                iconImageView.visibility = VISIBLE
                titleTextView.text = barcode.title
                openImageView.visibility = if (barcode.intent != null) VISIBLE else GONE
                copyImageView.visibility = VISIBLE
                barcode.details.let {
                    detailsTextView.visibility = if (it.isNullOrBlank()) GONE else VISIBLE
                    detailsTextView.text = it
                }
                barcode.value.let {
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
        if (mode == AUTO) {
            holder.update(null, mode)
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

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean = when {
        keyCode == KeyEvent.KEYCODE_BACK && decoder.toggle() -> reset()
        else -> super.onKeyLongPress(keyCode, event)
    }

    override fun onBackPressed() {
        if (reset()) return
        super.onBackPressed()
    }

}
