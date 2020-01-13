package fr.smarquis.qrcode.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color.BLACK
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import fr.smarquis.qrcode.R
import fr.smarquis.qrcode.model.Barcode
import kotlinx.android.synthetic.main.view_barcode.view.*

class BarcodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var onCollapsed: () -> Unit
    private lateinit var open: (Barcode) -> Unit
    private lateinit var copy: (Barcode) -> Unit
    private var more: (() -> Unit)? = null

    var barcode: Barcode? = null
        set(value) {
            field = value
            render(value)
        }

    private val callback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == STATE_COLLAPSED) {
                onCollapsed()
            }
        }
    }

    private fun bottomSheetBehavior() = BottomSheetBehavior.from(this)

    init {
        isClickable = true
        clipChildren = false
        orientation = VERTICAL
        View.inflate(context, R.layout.view_barcode, this)
        progressBar.indeterminateDrawable?.colorFilter = if (SDK_INT >= Q) BlendModeColorFilter(BLACK, BlendMode.SRC_IN) else PorterDuffColorFilter(BLACK, PorterDuff.Mode.SRC_IN)
        moreImageView.setOnClickListener { more?.invoke() }
        openImageView.setOnClickListener { barcode?.let { open(it) } }
        copyImageView.setOnClickListener { barcode?.let { copy(it) } }
        headerLinearLayout.setOnClickListener {
            barcode ?: return@setOnClickListener
            bottomSheetBehavior().state = when (bottomSheetBehavior().state) {
                STATE_COLLAPSED -> BottomSheetBehavior.STATE_EXPANDED
                BottomSheetBehavior.STATE_EXPANDED -> STATE_COLLAPSED
                else -> return@setOnClickListener
            }
        }
    }

    fun anchor(): ImageView = moreImageView

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bottomSheetBehavior().addBottomSheetCallback(callback)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        bottomSheetBehavior().removeBottomSheetCallback(callback)
    }

    fun configure(onCollapsed: () -> Unit, more: (() -> Unit)? = null, open: (Barcode) -> Unit, copy: (Barcode) -> Unit) {
        this.onCollapsed = onCollapsed
        this.more = more.also {
            moreImageView.visibility = if (it == null) View.GONE else View.VISIBLE
        }
        this.open = open
        this.copy = copy
    }

    private fun render(value: Barcode?) {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED -> {
                bottomSheetBehavior().isHideable = true
                bottomSheetBehavior().state = BottomSheetBehavior.STATE_HIDDEN
            }
            value == null -> {
                bottomSheetBehavior().state = STATE_COLLAPSED
                bottomSheetBehavior().isHideable = false
                progressBar.visibility = View.VISIBLE
                iconImageView.visibility = View.GONE
                iconImageView.setImageResource(0)
                titleTextView.setText(R.string.status_scanning)
                openImageView.visibility = View.GONE
                copyImageView.visibility = View.GONE
                detailsTextView.visibility = View.GONE
                rawTextView.visibility = View.GONE
            }
            else -> {
                if (bottomSheetBehavior().state == BottomSheetBehavior.STATE_EXPANDED) TransitionManager.beginDelayedTransition(this)
                progressBar.visibility = View.GONE
                iconImageView.setImageResource(value.icon)
                iconImageView.visibility = View.VISIBLE
                titleTextView.text = value.title
                openImageView.visibility = if (value.intent != null) View.VISIBLE else View.GONE
                copyImageView.visibility = View.VISIBLE
                value.details.let {
                    detailsTextView.visibility = if (it.isNullOrBlank()) View.GONE else View.VISIBLE
                    detailsTextView.text = it
                }
                value.value.let {
                    rawTextView.visibility = if (it.isBlank()) View.GONE else View.VISIBLE
                    rawTextView.text = it
                }
                bottomSheetBehavior().state = BottomSheetBehavior.STATE_EXPANDED
                bottomSheetBehavior().isHideable = false
            }
        }
    }

}
