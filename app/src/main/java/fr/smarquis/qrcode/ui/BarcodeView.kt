package fr.smarquis.qrcode.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import fr.smarquis.qrcode.R
import fr.smarquis.qrcode.databinding.ViewBarcodeBinding
import fr.smarquis.qrcode.model.Barcode

class BarcodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewBarcodeBinding.inflate(LayoutInflater.from(context), this)

    private val bottomSheetBehavior by lazy { BottomSheetBehavior.from(binding.root) }

    private lateinit var onCollapsed: () -> Unit
    private lateinit var open: (Barcode) -> Unit
    private lateinit var copy: (Barcode) -> Unit
    private var more: (() -> Unit)? = null

    var barcode: Barcode? = null
        set(value) {
            field = value.also { if (it == field) return }
            TransitionManager.beginDelayedTransition(this@BarcodeView)
            if (value != null) render(value) else clear()
        }

    private val callback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == STATE_COLLAPSED) {
                onCollapsed()
            }
        }
    }

    init {
        isClickable = true
        clipChildren = false
        orientation = VERTICAL
        with(binding) {
            moreImageView.setOnClickListener { more?.invoke() }
            openImageView.setOnClickListener { barcode?.let { open(it) } }
            copyImageView.setOnClickListener { barcode?.let { copy(it) } }
            headerLinearLayout.setOnClickListener {
                barcode ?: return@setOnClickListener
                bottomSheetBehavior.state = when (bottomSheetBehavior.state) {
                    STATE_COLLAPSED -> STATE_EXPANDED
                    STATE_EXPANDED -> STATE_COLLAPSED
                    else -> return@setOnClickListener
                }
            }
        }
    }

    fun anchor(): ImageView = binding.moreImageView

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bottomSheetBehavior.addBottomSheetCallback(callback)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        bottomSheetBehavior.removeBottomSheetCallback(callback)
    }

    fun configure(onCollapsed: () -> Unit, more: (() -> Unit)? = null, open: (Barcode) -> Unit, copy: (Barcode) -> Unit) {
        this.onCollapsed = onCollapsed
        this.more = more.also {
            binding.moreImageView.visibility = if (it == null) View.GONE else View.VISIBLE
        }
        this.open = open
        this.copy = copy
    }

    private fun clear() = with(binding) {
        bottomSheetBehavior.state = STATE_COLLAPSED
        progressBar.visibility = View.VISIBLE
        iconImageView.visibility = View.GONE
        iconImageView.setImageResource(0)
        titleTextView.setText(R.string.status_scanning)
        openImageView.visibility = View.GONE
        copyImageView.visibility = View.GONE
        detailsTextView.visibility = View.GONE
        rawTextView.visibility = View.GONE
    }

    private fun render(barcode: Barcode) = with(binding) {
        progressBar.visibility = View.GONE
        iconImageView.setImageResource(barcode.icon)
        iconImageView.visibility = View.VISIBLE
        titleTextView.text = barcode.title
        openImageView.visibility = if (barcode.intent != null) View.VISIBLE else View.GONE
        copyImageView.visibility = View.VISIBLE
        barcode.details.let {
            detailsTextView.visibility = if (it.isNullOrBlank()) View.GONE else View.VISIBLE
            detailsTextView.text = it
        }
        barcode.value.let {
            rawTextView.visibility = if (it.isBlank()) View.GONE else View.VISIBLE
            rawTextView.text = it
        }
        bottomSheetBehavior.state = STATE_EXPANDED
    }

}
