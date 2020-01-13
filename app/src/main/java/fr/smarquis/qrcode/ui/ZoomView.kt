package fr.smarquis.qrcode.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.KITKAT
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import io.fotoapparat.view.FocusView


class ZoomView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val input = 1F..5F
    private val output = 0F..1F
    private var scaleFactor = 1f
    private lateinit var focusView: FocusView
    private lateinit var zoomListener: ((Float) -> Unit)

    init {
        clipToPadding = false
        clipChildren = false
    }

    fun configure(zoom: (Float) -> Unit, focus: FocusView) {
        zoomListener = zoom
        focusView = focus
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        if (!scaleGestureDetector.isInProgress) {
            focusView.onTouchEvent(event)
        }
        return true
    }

    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor = (scaleFactor * detector.scaleFactor).coerceIn(input)
            zoomListener(map(scaleFactor, input, output))
            return true
        }
    }).apply {
        if (SDK_INT >= KITKAT) isQuickScaleEnabled = false
    }

    private fun map(value: Float, input: ClosedFloatingPointRange<Float>, output: ClosedFloatingPointRange<Float>) =
        output.start + (value - input.start) * (output.endInclusive - output.start) / (input.endInclusive - input.start)

}