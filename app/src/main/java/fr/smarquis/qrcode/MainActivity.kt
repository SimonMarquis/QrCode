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
        progress.indeterminateDrawable?.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)

        bottomSheetBehavior = from(bottom_sheet)
        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == STATE_COLLAPSED) {
                    holder.update(null, mode)
                    render()
                }
            }
        })

        action_open.setOnClickListener { safeStartIntent(this, holder.get()?.intent) }
        action_copy.setOnClickListener { copyToClipboard(this, holder.get()?.value) }

        title_textView.setOnClickListener {
            holder.get() ?: return@setOnClickListener
            bottomSheetBehavior.state = when (bottomSheetBehavior.state) {
                STATE_COLLAPSED -> STATE_EXPANDED
                STATE_EXPANDED -> STATE_COLLAPSED
                else -> return@setOnClickListener
            }
        }

        camera = Fotoapparat(
            context = this,
            view = findViewById<CameraView>(R.id.camera),
            focusView = findViewById<FocusView>(R.id.focus),
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
        root.playSoundEffect(SoundEffectConstants.CLICK)
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
                progress.visibility = VISIBLE
                icon_imageView.visibility = GONE
                icon_imageView.setImageResource(0)
                title_textView.text = getString(R.string.status_scanning_with, decoder.internal())
                action_open.visibility = GONE
                action_copy.visibility = GONE
                details_textView.visibility = GONE
                raw_textView.visibility = GONE
                bottomSheetBehavior.state = STATE_COLLAPSED
                bottomSheetBehavior.isHideable = false
            }
            else -> {
                if (bottomSheetBehavior.state == STATE_EXPANDED) TransitionManager.beginDelayedTransition(bottom_sheet)
                progress.visibility = GONE
                icon_imageView.setImageResource(barcode.icon)
                icon_imageView.visibility = VISIBLE
                title_textView.text = barcode.title
                action_open.visibility = if (barcode.intent != null) VISIBLE else GONE
                action_copy.visibility = VISIBLE
                barcode.details.let {
                    details_textView.visibility = if (it.isNullOrBlank()) GONE else VISIBLE
                    details_textView.text = it
                }
                barcode.value.let {
                    raw_textView.visibility = if (it.isBlank()) GONE else VISIBLE
                    raw_textView.text = it
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
